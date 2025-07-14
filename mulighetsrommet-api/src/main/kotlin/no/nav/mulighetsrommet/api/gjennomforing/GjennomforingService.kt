package no.nav.mulighetsrommet.api.gjennomforing

import arrow.core.*
import arrow.core.raise.either
import io.ktor.http.*
import io.ktor.server.response.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.endringshistorikk.EndringshistorikkDto
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingDbo
import no.nav.mulighetsrommet.api.gjennomforing.mapper.GjennomforingDboMapper
import no.nav.mulighetsrommet.api.gjennomforing.mapper.TiltaksgjennomforingEksternMapper
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingDto
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingStatusDto
import no.nav.mulighetsrommet.api.navansatt.service.NavAnsattService
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.responses.PaginatedResponse
import no.nav.mulighetsrommet.api.routes.v1.EksternTiltaksgjennomforingFilter
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.arena.ArenaMigrering.TiltaksgjennomforingSluttDatoCutoffDate
import no.nav.mulighetsrommet.database.utils.IntegrityConstraintViolation
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.notifications.ScheduledNotification
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class GjennomforingService(
    private val config: Config,
    private val db: ApiDatabase,
    private val validator: GjennomforingValidator,
    private val navAnsattService: NavAnsattService,
) {
    data class Config(
        val topic: String,
    )

    suspend fun upsert(
        request: GjennomforingRequest,
        navIdent: NavIdent,
        today: LocalDate = LocalDate.now(),
    ): Either<List<FieldError>, GjennomforingDto> = either {
        val previous = get(request.id)

        val status = resolveStatus(previous, request, today)
        val dbo = validator
            .validate(GjennomforingDboMapper.fromGjennomforingRequest(request, status), previous)
            .onRight { dbo ->
                dbo.kontaktpersoner.forEach {
                    navAnsattService.addUserToKontaktpersoner(it.navIdent)
                }
            }
            .bind()

        if (previous != null && GjennomforingDboMapper.fromGjennomforingDto(previous) == dbo) {
            return@either previous
        }

        db.transaction {
            queries.gjennomforing.upsert(dbo)

            dispatchNotificationToNewAdministrators(dbo, navIdent)

            val dto = getOrError(dbo.id)
            val operation = if (previous == null) {
                "Opprettet gjennomføring"
            } else {
                "Redigerte gjennomføring"
            }
            logEndring(operation, dto, navIdent)
            publishToKafka(dto)

            dto
        }
    }

    fun get(id: UUID): GjennomforingDto? = db.session {
        queries.gjennomforing.get(id)
    }

    fun getAll(
        pagination: Pagination,
        filter: AdminTiltaksgjennomforingFilter,
    ): PaginatedResponse<GjennomforingDto> = db.session {
        queries.gjennomforing.getAll(
            pagination,
            search = filter.search,
            navEnheter = filter.navEnheter,
            tiltakstypeIder = filter.tiltakstypeIder,
            statuser = filter.statuser,
            sortering = filter.sortering,
            avtaleId = filter.avtaleId,
            arrangorIds = filter.arrangorIds,
            administratorNavIdent = filter.administratorNavIdent,
            koordinatorNavIdent = filter.koordinatorNavIdent,
            publisert = filter.publisert,
            sluttDatoGreaterThanOrEqualTo = TiltaksgjennomforingSluttDatoCutoffDate,
        ).let { (totalCount, data) ->
            PaginatedResponse.of(pagination, totalCount, data)
        }
    }

    fun getEkstern(id: UUID): TiltaksgjennomforingEksternV1Dto? = db.session {
        queries.gjennomforing.get(id)?.let { dto ->
            TiltaksgjennomforingEksternMapper.fromGjennomforingDto(dto)
        }
    }

    fun getAllEkstern(
        pagination: Pagination,
        filter: EksternTiltaksgjennomforingFilter,
    ): PaginatedResponse<TiltaksgjennomforingEksternV1Dto> = db.session {
        queries.gjennomforing
            .getAll(
                pagination,
                arrangorOrgnr = filter.arrangorOrgnr,
            )
            .let { (totalCount, items) ->
                val data = items.map { dto -> TiltaksgjennomforingEksternMapper.fromGjennomforingDto(dto) }
                PaginatedResponse.of(pagination, totalCount, data)
            }
    }

    fun setPublisert(id: UUID, publisert: Boolean, navIdent: NavIdent): Unit = db.transaction {
        queries.gjennomforing.setPublisert(id, publisert)
        val dto = getOrError(id)
        val operation = if (publisert) {
            "Tiltak publisert"
        } else {
            "Tiltak avpublisert"
        }
        logEndring(operation, dto, navIdent)
    }

    fun setTilgjengeligForArrangorDato(
        id: UUID,
        tilgjengeligForArrangorDato: LocalDate,
        navIdent: NavIdent,
    ): Either<List<FieldError>, Unit> = db.transaction {
        val gjennomforing = getOrError(id)

        validator
            .validateTilgjengeligForArrangorDato(
                tilgjengeligForArrangorDato,
                gjennomforing.startDato,
            )
            .map {
                queries.gjennomforing.setTilgjengeligForArrangorDato(
                    id,
                    tilgjengeligForArrangorDato,
                )
                val dto = getOrError(id)
                val operation = "Endret dato for tilgang til Deltakeroversikten"
                logEndring(operation, dto, navIdent)
                publishToKafka(dto)
            }
    }

    fun avsluttGjennomforing(
        id: UUID,
        avsluttetTidspunkt: LocalDateTime,
        endretAv: Agent,
    ): Either<FieldError, GjennomforingDto> = db.transaction {
        val gjennomforing = getOrError(id)

        if (gjennomforing.status !is GjennomforingStatusDto.Gjennomfores) {
            return FieldError.root("Gjennomføringen må være aktiv for å kunne avsluttes").left()
        }

        val tidspunktForSlutt = gjennomforing.sluttDato?.plusDays(1)?.atStartOfDay()
        if (tidspunktForSlutt == null || avsluttetTidspunkt.isBefore(tidspunktForSlutt)) {
            return FieldError.root("Gjennomføringen kan ikke avsluttes før sluttdato").left()
        }

        queries.gjennomforing.setStatus(id, GjennomforingStatus.AVSLUTTET, avsluttetTidspunkt, null)
        queries.gjennomforing.setPublisert(id, false)
        queries.gjennomforing.setApentForPamelding(id, false)

        val dto = getOrError(id)
        logEndring("Gjennomføringen ble avsluttet", dto, endretAv)
        publishToKafka(dto)

        dto.right()
    }

    fun avbrytGjennomforing(
        id: UUID,
        avbruttTidspunkt: LocalDateTime,
        avbruttAarsak: AvbruttAarsak,
        endretAv: Agent,
    ): Either<FieldError, GjennomforingDto> = db.transaction {
        if (avbruttAarsak is AvbruttAarsak.Annet && avbruttAarsak.beskrivelse.length > 100) {
            return FieldError.root("Beskrivelse kan ikke inneholde mer enn 100 tegn").left()
        }
        if (avbruttAarsak is AvbruttAarsak.Annet && avbruttAarsak.beskrivelse.isEmpty()) {
            return FieldError.root("Beskrivelse er obligatorisk når “Annet” er valgt som årsak").left()
        }

        val gjennomforing = getOrError(id)

        when (gjennomforing.status) {
            is GjennomforingStatusDto.Gjennomfores -> Unit

            is GjennomforingStatusDto.Avlyst, is GjennomforingStatusDto.Avbrutt ->
                return FieldError.root("Gjennomføringen er allerede avbrutt").left()

            is GjennomforingStatusDto.Avsluttet ->
                return FieldError.root("Gjennomføringen er allerede avsluttet").left()
        }

        val tidspunktForStart = gjennomforing.startDato.atStartOfDay()
        val tidspunktForSlutt = gjennomforing.sluttDato?.plusDays(1)?.atStartOfDay()
        val status = if (avbruttTidspunkt.isBefore(tidspunktForStart)) {
            GjennomforingStatus.AVLYST
        } else if (tidspunktForSlutt == null || avbruttTidspunkt.isBefore(tidspunktForSlutt)) {
            GjennomforingStatus.AVBRUTT
        } else {
            return FieldError.root("Gjennomføringen kan ikke avbrytes etter at den er avsluttet").left()
        }

        queries.gjennomforing.setStatus(id, status, avbruttTidspunkt, avbruttAarsak)
        queries.gjennomforing.setPublisert(id, false)
        queries.gjennomforing.setApentForPamelding(id, false)

        val dto = getOrError(id)
        logEndring("Gjennomføringen ble avbrutt", dto, endretAv)
        publishToKafka(dto)

        dto.right()
    }

    fun setApentForPamelding(id: UUID, apentForPamelding: Boolean, agent: Agent): Unit = db.transaction {
        queries.gjennomforing.setApentForPamelding(id, apentForPamelding)

        val dto = getOrError(id)
        val operation = if (apentForPamelding) {
            "Åpnet for påmelding"
        } else {
            "Stengte for påmelding"
        }
        logEndring(operation, dto, agent)
        publishToKafka(dto)
    }

    fun setStengtHosArrangor(
        id: UUID,
        periode: Periode,
        beskrivelse: String,
        navIdent: NavIdent,
    ): Either<NonEmptyList<FieldError>, GjennomforingDto> = db.transaction {
        return query {
            queries.gjennomforing.setStengtHosArrangor(id, periode, beskrivelse)
        }.mapLeft {
            if (it is IntegrityConstraintViolation.ExclusionViolation) {
                FieldError.of(
                    SetStengtHosArrangorRequest::periodeStart,
                    "Perioden kan ikke overlappe med andre perioder",
                ).nel()
            } else {
                throw it.error
            }
        }.map {
            val dto = getOrError(id)
            val operation = listOf(
                "Registrerte stengt hos arrangør i perioden",
                periode.start.formaterDatoTilEuropeiskDatoformat(),
                "-",
                periode.getLastInclusiveDate().formaterDatoTilEuropeiskDatoformat(),
            ).joinToString(separator = " ")
            logEndring(operation, dto, navIdent)
            publishToKafka(dto)
            dto
        }
    }

    fun deleteStengtHosArrangor(id: UUID, periodeId: Int, navIdent: NavIdent) = db.transaction {
        queries.gjennomforing.deleteStengtHosArrangor(periodeId)

        val dto = getOrError(id)
        val operation = "Fjernet periode med stengt hos arrangør"
        logEndring(operation, dto, navIdent)
        publishToKafka(dto)
    }

    fun getEndringshistorikk(id: UUID): EndringshistorikkDto = db.session {
        return queries.endringshistorikk.getEndringshistorikk(DocumentClass.GJENNOMFORING, id)
    }

    fun frikobleKontaktpersonFraGjennomforing(
        kontaktpersonId: UUID,
        gjennomforingId: UUID,
        navIdent: NavIdent,
    ): Unit = db.transaction {
        queries.gjennomforing.frikobleKontaktpersonFraGjennomforing(
            kontaktpersonId = kontaktpersonId,
            gjennomforingId = gjennomforingId,
        )

        val gjennomforing = getOrError(gjennomforingId)
        logEndring(
            "Kontaktperson ble fjernet fra gjennomføringen via arrangørsidene",
            gjennomforing,
            navIdent,
        )
    }

    private fun resolveStatus(
        previous: GjennomforingDto?,
        request: GjennomforingRequest,
        today: LocalDate,
    ): GjennomforingStatus {
        return when (previous?.status) {
            is GjennomforingStatusDto.Avlyst, is GjennomforingStatusDto.Avbrutt -> previous.status.type
            else -> if (request.sluttDato == null || !request.sluttDato.isBefore(today)) {
                GjennomforingStatus.GJENNOMFORES
            } else {
                GjennomforingStatus.AVSLUTTET
            }
        }
    }

    private fun QueryContext.getOrError(id: UUID): GjennomforingDto {
        val gjennomforing = queries.gjennomforing.get(id)
        return requireNotNull(gjennomforing) { "Gjennomføringen med id=$id finnes ikke" }
    }

    private fun QueryContext.dispatchNotificationToNewAdministrators(
        dbo: GjennomforingDbo,
        navIdent: NavIdent,
    ) {
        val currentAdministratorer = get(dbo.id)?.administratorer?.map { it.navIdent }?.toSet()
            ?: setOf()

        val administratorsToNotify = (dbo.administratorer - currentAdministratorer - navIdent).toNonEmptyListOrNull()
            ?: return

        val notification = ScheduledNotification(
            title = "Du har blitt satt som administrator på gjennomføringen \"${dbo.navn}\"",
            targets = administratorsToNotify,
            createdAt = Instant.now(),
        )
        queries.notifications.insert(notification)
    }

    private fun QueryContext.logEndring(
        operation: String,
        dto: GjennomforingDto,
        endretAv: Agent,
    ) {
        queries.endringshistorikk.logEndring(
            DocumentClass.GJENNOMFORING,
            operation,
            endretAv,
            dto.id,
            LocalDateTime.now(),
        ) {
            Json.encodeToJsonElement(dto)
        }
    }

    private fun QueryContext.publishToKafka(dto: GjennomforingDto) {
        val eksternDto = TiltaksgjennomforingEksternMapper.fromGjennomforingDto(dto)

        val record = StoredProducerRecord(
            config.topic,
            eksternDto.id.toString().toByteArray(),
            Json.encodeToString(eksternDto).toByteArray(),
            null,
        )

        queries.kafkaProducerRecord.storeRecord(record)
    }
}
