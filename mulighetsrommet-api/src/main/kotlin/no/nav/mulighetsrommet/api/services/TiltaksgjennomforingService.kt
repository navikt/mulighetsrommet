package no.nav.mulighetsrommet.api.services

import arrow.core.Either
import arrow.core.left
import arrow.core.toNonEmptyListOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotliquery.TransactionalSession
import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.api.domain.dto.EndringshistorikkDto
import no.nav.mulighetsrommet.api.domain.dto.TiltaksgjennomforingDto
import no.nav.mulighetsrommet.api.domain.dto.TiltaksgjennomforingNotificationDto
import no.nav.mulighetsrommet.api.okonomi.tilsagn.TilsagnRepository
import no.nav.mulighetsrommet.api.repositories.AvtaleRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.responses.*
import no.nav.mulighetsrommet.api.routes.v1.AdminTiltaksgjennomforingFilter
import no.nav.mulighetsrommet.api.routes.v1.EksternTiltaksgjennomforingFilter
import no.nav.mulighetsrommet.api.routes.v1.TiltaksgjennomforingRequest
import no.nav.mulighetsrommet.api.tiltaksgjennomforinger.TiltaksgjennomforingValidator
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.domain.Tiltakskoder.isTiltakMedAvtalerFraMulighetsrommet
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering.TiltaksgjennomforingSluttDatoCutoffDate
import no.nav.mulighetsrommet.domain.dto.AvbruttAarsak
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingEksternV1Dto
import no.nav.mulighetsrommet.kafka.producers.SisteTiltaksgjennomforingerV1KafkaProducer
import no.nav.mulighetsrommet.notifications.NotificationRepository
import no.nav.mulighetsrommet.notifications.NotificationType
import no.nav.mulighetsrommet.notifications.ScheduledNotification
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class TiltaksgjennomforingService(
    private val avtaler: AvtaleRepository,
    private val tiltaksgjennomforinger: TiltaksgjennomforingRepository,
    private val tilsagn: TilsagnRepository,
    private val tiltaksgjennomforingKafkaProducer: SisteTiltaksgjennomforingerV1KafkaProducer,
    private val notificationRepository: NotificationRepository,
    private val validator: TiltaksgjennomforingValidator,
    private val documentHistoryService: EndringshistorikkService,
    private val tiltakstypeService: TiltakstypeService,
    private val navAnsattService: NavAnsattService,
    private val db: Database,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun upsert(
        request: TiltaksgjennomforingRequest,
        navIdent: NavIdent,
    ): Either<List<ValidationError>, TiltaksgjennomforingDto> {
        val previous = tiltaksgjennomforinger.get(request.id)
        return validator.validate(request.toDbo(), previous)
            .map { dbo ->
                db.transactionSuspend { tx ->
                    if (previous?.toTiltaksgjennomforingDbo() == dbo) {
                        return@transactionSuspend previous
                    }

                    dbo.kontaktpersoner.forEach {
                        navAnsattService.addUserToKontaktpersoner(it.navIdent, tx)
                    }

                    tiltaksgjennomforinger.upsert(dbo, tx)

                    val dto = getOrError(dbo.id, tx)

                    dispatchNotificationToNewAdministrators(tx, dbo, navIdent)
                    val operation = if (previous == null) {
                        "Opprettet gjennomføring"
                    } else {
                        "Redigerte gjennomføring"
                    }
                    logEndring(operation, dto, navIdent, tx)
                    tiltaksgjennomforingKafkaProducer.publish(dto.toTiltaksgjennomforingV1Dto())
                    dto
                }
            }
    }

    fun get(id: UUID): TiltaksgjennomforingDto? {
        return tiltaksgjennomforinger.get(id)
    }

    fun getAll(
        pagination: Pagination,
        filter: AdminTiltaksgjennomforingFilter,
    ): PaginatedResponse<TiltaksgjennomforingDto> = tiltaksgjennomforinger.getAll(
        pagination,
        search = filter.search,
        navEnheter = filter.navEnheter,
        tiltakstypeIder = filter.tiltakstypeIder,
        statuser = filter.statuser,
        sortering = filter.sortering,
        avtaleId = filter.avtaleId,
        arrangorIds = filter.arrangorIds,
        administratorNavIdent = filter.administratorNavIdent,
        publisert = filter.publisert,
        sluttDatoGreaterThanOrEqualTo = TiltaksgjennomforingSluttDatoCutoffDate,
    ).let { (totalCount, data) ->
        PaginatedResponse.of(pagination, totalCount, data)
    }

    fun getEkstern(id: UUID): TiltaksgjennomforingEksternV1Dto? {
        return tiltaksgjennomforinger.get(id)?.toTiltaksgjennomforingV1Dto()
    }

    fun getAllEkstern(
        pagination: Pagination,
        filter: EksternTiltaksgjennomforingFilter,
    ): PaginatedResponse<TiltaksgjennomforingEksternV1Dto> = tiltaksgjennomforinger
        .getAll(
            pagination,
            arrangorOrgnr = filter.arrangorOrgnr,
        )
        .let { (totalCount, items) ->
            val data = items.map { dto -> dto.toTiltaksgjennomforingV1Dto() }
            PaginatedResponse.of(pagination, totalCount, data)
        }

    fun getAllGjennomforingerSomNarmerSegSluttdato(): List<TiltaksgjennomforingNotificationDto> {
        return tiltaksgjennomforinger.getAllGjennomforingerSomNarmerSegSluttdato()
    }

    fun setPublisert(id: UUID, publisert: Boolean, navIdent: NavIdent) {
        db.transaction { tx ->
            tiltaksgjennomforinger.setPublisert(tx, id, publisert)
            val dto = getOrError(id, tx)
            val operation = if (publisert) {
                "Tiltak publisert"
            } else {
                "Tiltak avpublisert"
            }
            logEndring(operation, dto, navIdent, tx)
        }
    }

    fun setTilgjengeligForArrangorDato(
        id: UUID,
        tilgjengeligForArrangorDato: LocalDate,
        navIdent: NavIdent,
    ): Either<List<ValidationError>, Unit> = db.transaction { tx ->
        val gjennomforing = getOrError(id, tx)

        validator
            .validateTilgjengeligForArrangorDato(
                tilgjengeligForArrangorDato,
                gjennomforing.startDato,
            )
            .map {
                tiltaksgjennomforinger.setTilgjengeligForArrangorFraOgMedDato(
                    tx,
                    id,
                    tilgjengeligForArrangorDato,
                )
                val dto = getOrError(id, tx)
                val operation = "Endret dato for tilgang til Deltakeroversikten"
                logEndring(operation, dto, navIdent, tx)
                tiltaksgjennomforingKafkaProducer.publish(dto.toTiltaksgjennomforingV1Dto())
            }
    }

    fun setAvtale(id: UUID, avtaleId: UUID?, navIdent: NavIdent): StatusResponse<Unit> {
        val gjennomforing = get(id) ?: return NotFound("Gjennomføringen finnes ikke").left()

        if (!isTiltakMedAvtalerFraMulighetsrommet(gjennomforing.tiltakstype.tiltakskode)) {
            return BadRequest("Avtale kan bare settes for tiltaksgjennomføringer av type AFT eller VTA").left()
        }

        if (avtaleId != null) {
            val avtale = avtaler.get(avtaleId)
                ?: return BadRequest("Avtale med id=$avtaleId finnes ikke").left()
            if (gjennomforing.tiltakstype.id != avtale.tiltakstype.id) {
                return BadRequest("Tiltaksgjennomføringen må ha samme tiltakstype som avtalen").left()
            }
        }

        db.transaction { tx ->
            tiltaksgjennomforinger.setAvtaleId(tx, id, avtaleId)
            val dto = getOrError(id, tx)
            logEndring("Endret avtale", dto, navIdent, tx)
        }

        return Either.Right(Unit)
    }

    fun avbrytGjennomforing(
        id: UUID,
        navIdent: NavIdent,
        aarsak: AvbruttAarsak?,
    ): StatusResponse<Unit> {
        if (aarsak == null) {
            return Either.Left(BadRequest(message = "Årsak mangler"))
        }

        val gjennomforing = get(id) ?: return Either.Left(NotFound("Gjennomføringen finnes ikke"))

        if (!tiltakstypeService.isEnabled(gjennomforing.tiltakstype.tiltakskode)) {
            return Either.Left(BadRequest(message = "Tiltakstype '${gjennomforing.tiltakstype.navn}' må avbrytes i Arena."))
        }

        if (aarsak is AvbruttAarsak.Annet && aarsak.name.length > 100) {
            return Either.Left(BadRequest(message = "Beskrivelse kan ikke inneholde mer enn 100 tegn"))
        }

        if (aarsak is AvbruttAarsak.Annet && aarsak.name.isEmpty()) {
            return Either.Left(BadRequest(message = "Beskrivelse er obligatorisk når “Annet” er valgt som årsak"))
        }

        if (!gjennomforing.isAktiv()) {
            return Either.Left(BadRequest(message = "Gjennomføringen er allerede avsluttet og kan derfor ikke avbrytes."))
        }

        val aktiveTilsagn = tilsagn.getByGjennomforingId(gjennomforing.id)
            .filter { it.annullertTidspunkt == null }
        if (aktiveTilsagn.isNotEmpty()) {
            return Either.Left(BadRequest(message = "Gjennomføringen har aktive tilsagn"))
        }

        db.transaction { tx ->
            tiltaksgjennomforinger.avbryt(tx, id, LocalDateTime.now(), aarsak)
            val dto = getOrError(id, tx)
            logEndring("Gjennomføring ble avbrutt", dto, navIdent, tx)
            tiltaksgjennomforingKafkaProducer.publish(dto.toTiltaksgjennomforingV1Dto())
        }

        return Either.Right(Unit)
    }

    fun batchApentForInnsokForAlleMedStarttdatoForDato(dagensDato: LocalDate) {
        db.transaction { tx ->
            val tiltak = tiltaksgjennomforinger.lukkApentForInnsokForTiltakMedStartdatoForDato(
                dagensDato,
                tx,
            )
            tiltak.forEach { gjennomforing ->
                logEndringSomSystembruker(
                    operation = "Stengte for innsøk",
                    gjennomforing,
                    tx,
                )
            }
            logger.info("Oppdaterte ${tiltak.size} tiltak med åpent for innsøk = false")
        }
    }

    fun getEndringshistorikk(id: UUID): EndringshistorikkDto {
        return documentHistoryService.getEndringshistorikk(DocumentClass.TILTAKSGJENNOMFORING, id)
    }

    private fun getOrError(id: UUID, tx: TransactionalSession): TiltaksgjennomforingDto {
        val gjennomforing = tiltaksgjennomforinger.get(id, tx)
        return requireNotNull(gjennomforing) { "Gjennomføringen med id=$id finnes ikke" }
    }

    private fun dispatchNotificationToNewAdministrators(
        tx: TransactionalSession,
        dbo: TiltaksgjennomforingDbo,
        navIdent: NavIdent,
    ) {
        val currentAdministratorer =
            get(dbo.id)?.administratorer?.map { it.navIdent }?.toSet() ?: setOf()

        val administratorsToNotify =
            (dbo.administratorer - currentAdministratorer - navIdent).toNonEmptyListOrNull()
                ?: return

        val notification = ScheduledNotification(
            type = NotificationType.NOTIFICATION,
            title = "Du har blitt satt som administrator på gjennomføringen \"${dbo.navn}\"",
            targets = administratorsToNotify,
            createdAt = Instant.now(),
        )
        notificationRepository.insert(notification, tx)
    }

    private fun logEndring(
        operation: String,
        dto: TiltaksgjennomforingDto,
        navIdent: NavIdent,
        tx: TransactionalSession,
    ) {
        documentHistoryService.logEndring(
            tx,
            DocumentClass.TILTAKSGJENNOMFORING,
            operation,
            navIdent.value,
            dto.id,
        ) {
            Json.encodeToJsonElement(dto)
        }
    }

    private fun logEndringSomSystembruker(
        operation: String,
        dto: TiltaksgjennomforingDto,
        tx: TransactionalSession,
    ) {
        documentHistoryService.logEndring(
            tx,
            DocumentClass.TILTAKSGJENNOMFORING,
            operation,
            TILTAKSADMINISTRASJON_SYSTEM_BRUKER,
            dto.id,
        ) {
            Json.encodeToJsonElement(dto)
        }
    }

    fun frikobleKontaktpersonFraGjennomforing(
        kontaktpersonId: UUID,
        gjennomforingId: UUID,
        navIdent: NavIdent,
    ): Either<StatusResponseError, String> {
        val gjennomforing =
            tiltaksgjennomforinger.get(gjennomforingId)
                ?: return Either.Left(NotFound("Gjennomføringen finnes ikke"))

        return db.transaction { tx ->
            tiltaksgjennomforinger.frikobleKontaktpersonFraGjennomforing(
                kontaktpersonId = kontaktpersonId,
                gjennomforingId = gjennomforingId,
                tx = tx,
            ).map {
                logEndring(
                    "Kontaktperson ble fjernet fra gjennomføringen via arrangørsidene",
                    gjennomforing,
                    navIdent,
                    tx,
                )
                it
            }.mapLeft {
                logger.error("Klarte ikke fjerne kontaktperson fra gjennomføring: KontaktpersonId = '$kontaktpersonId', gjennomforingId = '$gjennomforingId'")
                ServerError("Klarte ikke fjerne kontaktperson fra gjennomføringen")
            }
        }
    }
}
