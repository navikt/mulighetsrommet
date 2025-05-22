package no.nav.mulighetsrommet.api.gjennomforing

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.nel
import arrow.core.raise.either
import arrow.core.toNonEmptyListOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.endringshistorikk.EndringshistorikkDto
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingDbo
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingKontaktpersonDbo
import no.nav.mulighetsrommet.api.gjennomforing.mapper.GjennomforingDboMapper
import no.nav.mulighetsrommet.api.gjennomforing.mapper.TiltaksgjennomforingEksternMapper
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingDto
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
    ): Either<List<FieldError>, GjennomforingDto> = either {
        val previous = get(request.id)

        val dbo = validator.validate(toDbo(request), previous)
            .onRight { dbo ->
                dbo.kontaktpersoner.forEach {
                    navAnsattService.addUserToKontaktpersoner(it.navIdent)
                }
            }
            .bind()

        if (previous != null && GjennomforingDboMapper.toTiltaksgjennomforingDbo(previous) == dbo) {
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
            TiltaksgjennomforingEksternMapper.toTiltaksgjennomforingV1Dto(dto)
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
                val data = items.map { dto -> TiltaksgjennomforingEksternMapper.toTiltaksgjennomforingV1Dto(dto) }
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

    fun setAvtale(id: UUID, avtaleId: UUID?, navIdent: NavIdent): Unit = db.transaction {
        queries.gjennomforing.setAvtaleId(id, avtaleId)
        val dto = getOrError(id)
        logEndring("Endret avtale", dto, navIdent)
    }

    fun setAvsluttet(
        id: UUID,
        avsluttetTidspunkt: LocalDateTime,
        avsluttetAarsak: AvbruttAarsak?,
        endretAv: Agent,
    ): Unit = db.transaction {
        queries.gjennomforing.setAvsluttet(id, avsluttetTidspunkt, avsluttetAarsak)

        val dto = getOrError(id)
        val operation = when (dto.status.status) {
            GjennomforingStatus.AVSLUTTET,
            GjennomforingStatus.AVBRUTT,
            GjennomforingStatus.AVLYST,
            -> "Gjennomføringen ble ${dto.status.status.name.lowercase()}"

            else -> throw IllegalStateException("Gjennomføringen ble nettopp avsluttet, men status er fortsatt ${dto.status.status}")
        }
        logEndring(operation, dto, endretAv)

        publishToKafka(dto)
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
        val eksternDto = TiltaksgjennomforingEksternMapper.toTiltaksgjennomforingV1Dto(dto)

        val record = StoredProducerRecord(
            config.topic,
            eksternDto.id.toString().toByteArray(),
            Json.encodeToString(eksternDto).toByteArray(),
            null,
        )

        queries.kafkaProducerRecord.storeRecord(record)
    }
}

private fun toDbo(request: GjennomforingRequest) = request.run {
    GjennomforingDbo(
        id = id,
        navn = navn,
        tiltakstypeId = tiltakstypeId,
        avtaleId = avtaleId,
        startDato = startDato,
        sluttDato = sluttDato,
        antallPlasser = antallPlasser,
        arrangorId = arrangorId,
        arrangorKontaktpersoner = arrangorKontaktpersoner,
        administratorer = administratorer,
        navEnheter = navEnheter,
        oppstart = oppstart,
        kontaktpersoner = kontaktpersoner.map {
            GjennomforingKontaktpersonDbo(
                navIdent = it.navIdent,
                navEnheter = it.navEnheter,
                beskrivelse = it.beskrivelse,
            )
        },
        stedForGjennomforing = stedForGjennomforing,
        faneinnhold = faneinnhold,
        beskrivelse = beskrivelse,
        deltidsprosent = deltidsprosent,
        estimertVentetidVerdi = estimertVentetid?.verdi,
        estimertVentetidEnhet = estimertVentetid?.enhet,
        tilgjengeligForArrangorDato = tilgjengeligForArrangorDato,
        amoKategorisering = amoKategorisering,
        utdanningslop = utdanningslop,
    )
}
