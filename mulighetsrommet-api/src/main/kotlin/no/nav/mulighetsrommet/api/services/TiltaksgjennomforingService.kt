package no.nav.mulighetsrommet.api.services

import arrow.core.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotliquery.TransactionalSession
import no.nav.mulighetsrommet.api.clients.vedtak.Innsatsgruppe
import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.api.domain.dto.*
import no.nav.mulighetsrommet.api.repositories.AvtaleRepository
import no.nav.mulighetsrommet.api.repositories.DeltakerRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.UtkastRepository
import no.nav.mulighetsrommet.api.routes.v1.TiltaksgjennomforingRequest
import no.nav.mulighetsrommet.api.routes.v1.responses.*
import no.nav.mulighetsrommet.api.tiltaksgjennomforinger.TiltaksgjennomforingValidator
import no.nav.mulighetsrommet.api.utils.AdminTiltaksgjennomforingFilter
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.Tiltakskoder.isTiltakMedAvtalerFraMulighetsrommet
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.kafka.producers.TiltaksgjennomforingKafkaProducer
import no.nav.mulighetsrommet.notifications.NotificationRepository
import no.nav.mulighetsrommet.notifications.NotificationType
import no.nav.mulighetsrommet.notifications.ScheduledNotification
import java.time.Instant
import java.util.*

class TiltaksgjennomforingService(
    private val avtaler: AvtaleRepository,
    private val tiltaksgjennomforinger: TiltaksgjennomforingRepository,
    private val deltakerRepository: DeltakerRepository,
    private val virksomhetService: VirksomhetService,
    private val utkastRepository: UtkastRepository,
    private val tiltaksgjennomforingKafkaProducer: TiltaksgjennomforingKafkaProducer,
    private val notificationRepository: NotificationRepository,
    private val validator: TiltaksgjennomforingValidator,
    private val documentHistoryService: EndringshistorikkService,
    private val db: Database,
) {
    suspend fun upsert(
        request: TiltaksgjennomforingRequest,
        navIdent: String,
    ): Either<List<ValidationError>, TiltaksgjennomforingAdminDto> {
        val previous = tiltaksgjennomforinger.get(request.id)
        return virksomhetService.getOrSyncHovedenhetFromBrreg(request.arrangorOrganisasjonsnummer)
            .mapLeft {
                ValidationError
                    .of(
                        TiltaksgjennomforingDbo::arrangorOrganisasjonsnummer,
                        "Arrangøren finnes ikke Brønnøysundregistrene",
                    )
                    .nel()
            }
            .flatMap {
                validator.validate(request.toDbo(), previous)
            }
            .map { dbo ->
                db.transactionSuspend { tx ->
                    if (previous?.toDbo() == dbo) {
                        return@transactionSuspend previous
                    }

                    tiltaksgjennomforinger.upsert(dbo, tx)
                    utkastRepository.delete(dbo.id, tx)

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
        navRegioner = filter.navRegioner,
        avtaleId = filter.avtaleId,
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
        navRegioner = filter.navRegioner,
        avtaleId = filter.avtaleId,
        arrangorOrgnr = filter.arrangorOrgnr,
        administratorNavIdent = filter.administratorNavIdent,
    ).let { (totalCount, data) ->
        PaginatedResponse.of(pagination, totalCount, data)
    }

    fun getAllGjennomforingerSomNarmerSegSluttdato(): List<TiltaksgjennomforingNotificationDto> {
        return tiltaksgjennomforinger.getAllGjennomforingerSomNarmerSegSluttdato()
    }

    fun getAllMidlertidigStengteGjennomforingerSomNarmerSegSluttdato(): List<TiltaksgjennomforingNotificationDto> {
        return tiltaksgjennomforinger.getAllMidlertidigStengteGjennomforingerSomNarmerSegSluttdato()
    }

    fun setPublisert(id: UUID, publisert: Boolean, navIdent: String) {
        db.transaction { tx ->
            tiltaksgjennomforinger.setPublisert(tx, id, publisert)
            val dto = getOrError(id, tx)
            val operation = if (publisert) {
                "Tiltak publisert"
            } else {
                "Tiltak ikke publisert"
            }
            logEndring(operation, dto, navIdent, tx)
        }
    }

    fun setAvtale(id: UUID, avtaleId: UUID?, navIdent: String): StatusResponse<Unit> {
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

    fun avbrytGjennomforing(id: UUID, navIdent: String): StatusResponse<Unit> {
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
        navIdent: String,
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
        navIdent: String,
        tx: TransactionalSession,
    ) {
        documentHistoryService.logEndring(tx, DocumentClass.TILTAKSGJENNOMFORING, operation, navIdent, dto.id) {
            Json.encodeToJsonElement<TiltaksgjennomforingAdminDto>(dto)
        }
    }
}
