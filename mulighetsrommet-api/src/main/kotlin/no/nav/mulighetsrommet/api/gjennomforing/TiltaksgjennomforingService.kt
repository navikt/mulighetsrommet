package no.nav.mulighetsrommet.api.gjennomforing

import arrow.core.Either
import arrow.core.toNonEmptyListOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotliquery.TransactionalSession
import no.nav.mulighetsrommet.api.domain.dto.EndringshistorikkDto
import no.nav.mulighetsrommet.api.gjennomforing.db.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.api.gjennomforing.db.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.gjennomforing.kafka.SisteTiltaksgjennomforingerV1KafkaProducer
import no.nav.mulighetsrommet.api.gjennomforing.model.TiltaksgjennomforingDto
import no.nav.mulighetsrommet.api.navansatt.NavAnsattService
import no.nav.mulighetsrommet.api.responses.PaginatedResponse
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.routes.v1.EksternTiltaksgjennomforingFilter
import no.nav.mulighetsrommet.api.services.DocumentClass
import no.nav.mulighetsrommet.api.services.EndretAv
import no.nav.mulighetsrommet.api.services.EndringshistorikkService
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering.TiltaksgjennomforingSluttDatoCutoffDate
import no.nav.mulighetsrommet.domain.dto.AvbruttAarsak
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingEksternV1Dto
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingStatus
import no.nav.mulighetsrommet.notifications.NotificationRepository
import no.nav.mulighetsrommet.notifications.NotificationType
import no.nav.mulighetsrommet.notifications.ScheduledNotification
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class TiltaksgjennomforingService(
    private val tiltaksgjennomforinger: TiltaksgjennomforingRepository,
    private val tiltaksgjennomforingKafkaProducer: SisteTiltaksgjennomforingerV1KafkaProducer,
    private val notificationRepository: NotificationRepository,
    private val validator: TiltaksgjennomforingValidator,
    private val documentHistoryService: EndringshistorikkService,
    private val navAnsattService: NavAnsattService,
    private val db: Database,
) {
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
                    logEndring(operation, dto, EndretAv.NavAnsatt(navIdent), tx)
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

    fun setPublisert(id: UUID, publisert: Boolean, navIdent: NavIdent) {
        db.transaction { tx ->
            tiltaksgjennomforinger.setPublisert(tx, id, publisert)
            val dto = getOrError(id, tx)
            val operation = if (publisert) {
                "Tiltak publisert"
            } else {
                "Tiltak avpublisert"
            }
            logEndring(operation, dto, EndretAv.NavAnsatt(navIdent), tx)
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
                logEndring(operation, dto, EndretAv.NavAnsatt(navIdent), tx)
                tiltaksgjennomforingKafkaProducer.publish(dto.toTiltaksgjennomforingV1Dto())
            }
    }

    fun setAvtale(id: UUID, avtaleId: UUID?, navIdent: NavIdent): Unit = db.transaction { tx ->
        tiltaksgjennomforinger.setAvtaleId(tx, id, avtaleId)
        val dto = getOrError(id, tx)
        logEndring("Endret avtale", dto, EndretAv.NavAnsatt(navIdent), tx)
    }

    fun setAvsluttet(
        id: UUID,
        avsluttetTidspunkt: LocalDateTime,
        avsluttetAarsak: AvbruttAarsak?,
        endretAv: EndretAv,
    ): Unit = db.transaction { tx ->
        tiltaksgjennomforinger.setAvsluttet(tx, id, avsluttetTidspunkt, avsluttetAarsak)

        val dto = getOrError(id, tx)
        val operation = when (dto.status.status) {
            TiltaksgjennomforingStatus.AVSLUTTET,
            TiltaksgjennomforingStatus.AVBRUTT,
            TiltaksgjennomforingStatus.AVLYST,
            -> "Gjennomføringen ble ${dto.status.status.name.lowercase()}"

            else -> throw IllegalStateException("Gjennomføringen ble nettopp avsluttet, men status er fortsatt ${dto.status.status}")
        }
        logEndring(operation, dto, endretAv, tx)

        tiltaksgjennomforingKafkaProducer.publish(dto.toTiltaksgjennomforingV1Dto())
    }

    fun setApentForPamelding(id: UUID, apentForPamelding: Boolean, bruker: EndretAv) = db.transaction { tx ->
        tiltaksgjennomforinger.setApentForPamelding(tx, id, apentForPamelding)

        val dto = getOrError(id, tx)
        val operation = if (apentForPamelding) {
            "Åpnet for påmelding"
        } else {
            "Stengte for påmelding"
        }
        logEndring(operation, dto, bruker, tx)

        tiltaksgjennomforingKafkaProducer.publish(dto.toTiltaksgjennomforingV1Dto())
    }

    fun getEndringshistorikk(id: UUID): EndringshistorikkDto {
        return documentHistoryService.getEndringshistorikk(DocumentClass.TILTAKSGJENNOMFORING, id)
    }

    fun frikobleKontaktpersonFraGjennomforing(
        kontaktpersonId: UUID,
        gjennomforingId: UUID,
        navIdent: NavIdent,
    ): Unit = db.transaction { tx ->
        tiltaksgjennomforinger.frikobleKontaktpersonFraGjennomforing(
            tx = tx,
            kontaktpersonId = kontaktpersonId,
            gjennomforingId = gjennomforingId,
        )

        val gjennomforing = getOrError(gjennomforingId, tx)
        logEndring(
            "Kontaktperson ble fjernet fra gjennomføringen via arrangørsidene",
            gjennomforing,
            EndretAv.NavAnsatt(navIdent),
            tx,
        )
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
        endretAv: EndretAv,
        tx: TransactionalSession,
    ) {
        documentHistoryService.logEndring(
            tx,
            DocumentClass.TILTAKSGJENNOMFORING,
            operation,
            endretAv,
            dto.id,
        ) {
            Json.encodeToJsonElement(dto)
        }
    }
}
