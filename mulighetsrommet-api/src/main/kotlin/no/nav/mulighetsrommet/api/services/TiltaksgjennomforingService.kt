package no.nav.mulighetsrommet.api.services

import arrow.core.Either
import arrow.core.left
import arrow.core.toNonEmptyListOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotliquery.TransactionalSession
import no.nav.mulighetsrommet.api.clients.vedtak.Innsatsgruppe
import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.api.domain.dto.*
import no.nav.mulighetsrommet.api.repositories.AvtaleRepository
import no.nav.mulighetsrommet.api.repositories.DeltakerRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.routes.v1.TiltaksgjennomforingRequest
import no.nav.mulighetsrommet.api.routes.v1.responses.*
import no.nav.mulighetsrommet.api.tiltaksgjennomforinger.TiltaksgjennomforingValidator
import no.nav.mulighetsrommet.api.utils.AdminTiltaksgjennomforingFilter
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.Tiltakskoder.isTiltakMedAvtalerFraMulighetsrommet
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.kafka.producers.TiltaksgjennomforingKafkaProducer
import no.nav.mulighetsrommet.notifications.NotificationRepository
import no.nav.mulighetsrommet.notifications.NotificationType
import no.nav.mulighetsrommet.notifications.ScheduledNotification
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDate
import java.util.*

class TiltaksgjennomforingService(
    private val avtaler: AvtaleRepository,
    private val tiltaksgjennomforinger: TiltaksgjennomforingRepository,
    private val deltakerRepository: DeltakerRepository,
    private val tiltaksgjennomforingKafkaProducer: TiltaksgjennomforingKafkaProducer,
    private val notificationRepository: NotificationRepository,
    private val validator: TiltaksgjennomforingValidator,
    private val documentHistoryService: EndringshistorikkService,
    private val db: Database,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    suspend fun upsert(
        request: TiltaksgjennomforingRequest,
        navIdent: NavIdent,
    ): Either<List<ValidationError>, TiltaksgjennomforingAdminDto> {
        val previous = tiltaksgjennomforinger.get(request.id)
        return validator.validate(request.toDbo(), previous)
            .map { dbo ->
                db.transactionSuspend { tx ->
                    if (previous?.toDbo() == dbo) {
                        return@transactionSuspend previous
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
                    tiltaksgjennomforingKafkaProducer.publish(TiltaksgjennomforingDto.from(dto))
                    dto
                }
            }
    }

    fun get(id: UUID): TiltaksgjennomforingAdminDto? {
        return tiltaksgjennomforinger.get(id)
    }

    fun getAllSkalMigreres(
        pagination: PaginationParams,
        filter: AdminTiltaksgjennomforingFilter,
    ): PaginatedResponse<TiltaksgjennomforingAdminDto> = tiltaksgjennomforinger.getAll(
        pagination,
        search = filter.search,
        navEnheter = filter.navEnheter,
        tiltakstypeIder = filter.tiltakstypeIder,
        statuser = filter.statuser,
        sortering = filter.sortering,
        sluttDatoCutoff = filter.sluttDatoCutoff,
        dagensDato = filter.dagensDato,
        avtaleId = filter.avtaleId,
        arrangorIds = filter.arrangorIds,
        arrangorOrgnr = filter.arrangorOrgnr,
        administratorNavIdent = filter.administratorNavIdent,
        skalMigreres = true,
    ).let { (totalCount, data) ->
        PaginatedResponse.of(pagination, totalCount, data)
    }

    fun getAllVeilederflateTiltaksgjennomforing(
        search: String?,
        apentForInnsok: Boolean?,
        sanityTiltakstypeIds: List<UUID>?,
        innsatsgrupper: List<Innsatsgruppe>,
        enheter: List<String>,
    ): List<VeilederflateTiltaksgjennomforing> = tiltaksgjennomforinger.getAllVeilederflateTiltaksgjennomforing(
        search,
        apentForInnsok,
        sanityTiltakstypeIds,
        innsatsgrupper,
        enheter,
    )

    fun getAll(
        pagination: PaginationParams,
        filter: AdminTiltaksgjennomforingFilter,
    ): PaginatedResponse<TiltaksgjennomforingAdminDto> = tiltaksgjennomforinger.getAll(
        pagination,
        search = filter.search,
        navEnheter = filter.navEnheter,
        tiltakstypeIder = filter.tiltakstypeIder,
        statuser = filter.statuser,
        sortering = filter.sortering,
        sluttDatoCutoff = filter.sluttDatoCutoff,
        dagensDato = filter.dagensDato,
        avtaleId = filter.avtaleId,
        arrangorIds = filter.arrangorIds,
        arrangorOrgnr = filter.arrangorOrgnr,
        administratorNavIdent = filter.administratorNavIdent,
    ).let { (totalCount, data) ->
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

    fun setAvtale(id: UUID, avtaleId: UUID?, navIdent: NavIdent): StatusResponse<Unit> {
        val gjennomforing = get(id) ?: return NotFound("Gjennomføringen finnes ikke").left()

        if (!isTiltakMedAvtalerFraMulighetsrommet(gjennomforing.tiltakstype.arenaKode)) {
            return BadRequest("Avtale kan bare settes for tiltaksgjennomføringer av type AFT eller VTA").left()
        }

        if (avtaleId != null) {
            val avtale = avtaler.get(avtaleId) ?: return BadRequest("Avtale med id=$avtaleId finnes ikke").left()
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

    fun avbrytGjennomforing(id: UUID, navIdent: NavIdent): StatusResponse<Unit> {
        val gjennomforing = get(id) ?: return Either.Left(NotFound("Gjennomføringen finnes ikke"))

        if (gjennomforing.opphav == ArenaMigrering.Opphav.ARENA) {
            return Either.Left(BadRequest(message = "Gjennomføringen har opprinnelse fra Arena og kan ikke bli avbrutt i admin-flate."))
        }

        if (!gjennomforing.isAktiv()) {
            return Either.Left(BadRequest(message = "Gjennomføringen kan ikke avbrytes fordi den allerede er avsluttet."))
        }

        val antallDeltagere = deltakerRepository.getAll(id).size
        if (antallDeltagere > 0) {
            return Either.Left(BadRequest(message = "Gjennomføringen kan ikke avbrytes fordi den har $antallDeltagere deltager(e) koblet til seg."))
        }

        db.transaction { tx ->
            tiltaksgjennomforinger.setAvslutningsstatus(tx, id, Avslutningsstatus.AVBRUTT)
            val dto = getOrError(id, tx)
            logEndring("Gjennomføring ble avbrutt", dto, navIdent, tx)
            tiltaksgjennomforingKafkaProducer.publish(TiltaksgjennomforingDto.from(dto))
        }

        return Either.Right(Unit)
    }

    fun batchApentForInnsokForAlleMedStarttdatoForDato(dagensDato: LocalDate) {
        db.transaction { tx ->
            val tiltak = tiltaksgjennomforinger.lukkApentForInnsokForTiltakMedStartdatoForDato(dagensDato, tx)
            tiltak.forEach {
                logEndringSomSystembruker(
                    operation = "Stengte for innsøk",
                    it,
                    tx,
                )
            }
            logger.info("Oppdaterte ${tiltak.size} tiltak med åpent for innsøk = false")
        }
    }

    fun getEndringshistorikk(id: UUID): EndringshistorikkDto {
        return documentHistoryService.getEndringshistorikk(DocumentClass.TILTAKSGJENNOMFORING, id)
    }

    private fun getOrError(id: UUID, tx: TransactionalSession): TiltaksgjennomforingAdminDto {
        val gjennomforing = tiltaksgjennomforinger.get(id, tx)
        return requireNotNull(gjennomforing) { "Gjennomføringen med id=$id finnes ikke" }
    }

    private fun dispatchNotificationToNewAdministrators(
        tx: TransactionalSession,
        dbo: TiltaksgjennomforingDbo,
        navIdent: NavIdent,
    ) {
        val currentAdministratorer = get(dbo.id)?.administratorer?.map { it.navIdent }?.toSet() ?: setOf()

        val administratorsToNotify =
            (dbo.administratorer - currentAdministratorer - navIdent).toNonEmptyListOrNull() ?: return

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
        dto: TiltaksgjennomforingAdminDto,
        navIdent: NavIdent,
        tx: TransactionalSession,
    ) {
        documentHistoryService.logEndring(tx, DocumentClass.TILTAKSGJENNOMFORING, operation, navIdent.value, dto.id) {
            Json.encodeToJsonElement<TiltaksgjennomforingAdminDto>(dto)
        }
    }

    private fun logEndringSomSystembruker(
        operation: String,
        dto: TiltaksgjennomforingAdminDto,
        tx: TransactionalSession,
    ) {
        documentHistoryService.logEndring(
            tx,
            DocumentClass.TILTAKSGJENNOMFORING,
            operation,
            TILTAKSADMINISTRASJON_SYSTEM_BRUKER,
            dto.id,
        ) {
            Json.encodeToJsonElement<TiltaksgjennomforingAdminDto>(dto)
        }
    }
}
