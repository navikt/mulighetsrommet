package no.nav.mulighetsrommet.api.services

import arrow.core.Either
import arrow.core.left
import arrow.core.toNonEmptyListOrNull
import io.ktor.server.plugins.*
import kotliquery.TransactionalSession
import no.nav.mulighetsrommet.api.clients.vedtak.Innsatsgruppe
import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.api.domain.dto.TiltaksgjennomforingAdminDto
import no.nav.mulighetsrommet.api.domain.dto.TiltaksgjennomforingDto
import no.nav.mulighetsrommet.api.domain.dto.TiltaksgjennomforingNotificationDto
import no.nav.mulighetsrommet.api.domain.dto.VeilederflateTiltaksgjennomforing
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
    private val db: Database,
) {
    suspend fun upsert(
        request: TiltaksgjennomforingRequest,
        navIdent: String,
    ): Either<List<ValidationError>, TiltaksgjennomforingAdminDto> {
        virksomhetService.getOrSyncVirksomhet(request.arrangorOrganisasjonsnummer)

        return validator.validate(request.toDbo())
            .map { dbo ->
                db.transactionSuspend { tx ->
                    tiltaksgjennomforinger.upsert(dbo, tx)
                    utkastRepository.delete(dbo.id, tx)

                    dispatchNotificationToNewAdministrators(tx, dbo, navIdent)

                    val dto = tiltaksgjennomforinger.get(dbo.id, tx)!!
                    tiltaksgjennomforingKafkaProducer.publish(TiltaksgjennomforingDto.from(dto))
                    dto
                }
            }
    }

    fun get(id: UUID): TiltaksgjennomforingAdminDto? = tiltaksgjennomforinger.get(id)

    fun getAllSkalMigreres(
        pagination: PaginationParams,
        filter: AdminTiltaksgjennomforingFilter,
    ): PaginatedResponse<TiltaksgjennomforingAdminDto> = tiltaksgjennomforinger
        .getAll(
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
        )
        .let { (totalCount, data) ->
            PaginatedResponse(
                pagination = Pagination(
                    totalCount = totalCount,
                    currentPage = pagination.page,
                    pageSize = pagination.limit,
                ),
                data = data,
            )
        }

    fun getAllVeilederflateTiltaksgjennomforing(
        search: String?,
        apentForInnsok: Boolean?,
        sanityTiltakstypeIds: List<UUID>?,
        innsatsgrupper: List<Innsatsgruppe>,
        brukersEnheter: List<String>,
    ): List<VeilederflateTiltaksgjennomforing> = tiltaksgjennomforinger.getAllVeilederflateTiltaksgjennomforing(
        search,
        apentForInnsok,
        sanityTiltakstypeIds,
        innsatsgrupper,
        brukersEnheter,
    )

    fun getAll(
        pagination: PaginationParams,
        filter: AdminTiltaksgjennomforingFilter,
    ): PaginatedResponse<TiltaksgjennomforingAdminDto> = tiltaksgjennomforinger
        .getAll(
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
        )
        .let { (totalCount, data) ->
            PaginatedResponse(
                pagination = Pagination(
                    totalCount = totalCount,
                    currentPage = pagination.page,
                    pageSize = pagination.limit,
                ),
                data = data,
            )
        }

    fun getAllGjennomforingerSomNarmerSegSluttdato(): List<TiltaksgjennomforingNotificationDto> {
        return tiltaksgjennomforinger.getAllGjennomforingerSomNarmerSegSluttdato()
    }

    fun getAllMidlertidigStengteGjennomforingerSomNarmerSegSluttdato(): List<TiltaksgjennomforingNotificationDto> {
        return tiltaksgjennomforinger.getAllMidlertidigStengteGjennomforingerSomNarmerSegSluttdato()
    }

    fun setTilgjengeligForVeileder(id: UUID, tilgjengeligForVeileder: Boolean) {
        val updatedRows = tiltaksgjennomforinger.setTilgjengeligForVeileder(id, tilgjengeligForVeileder)
        if (updatedRows != 1) {
            throw NotFoundException("Gjennomføringen finnes ikke")
        }
    }

    fun setAvtale(gjennomforingId: UUID, avtaleId: UUID?): StatusResponse<Unit> {
        val gjennomforing = tiltaksgjennomforinger.get(gjennomforingId)
            ?: return NotFound("Tiltaksgjennomføring med id=$gjennomforingId finnes ikke").left()

        if (!isTiltakMedAvtalerFraMulighetsrommet(gjennomforing.tiltakstype.arenaKode)) {
            return BadRequest("Avtale kan bare settes for tiltaksgjennomføringer av type AFT eller VTA").left()
        }

        if (avtaleId != null) {
            val avtale = avtaler.get(avtaleId) ?: return BadRequest("Avtale med id=$avtaleId finnes ikke").left()
            if (gjennomforing.tiltakstype.id != avtale.tiltakstype.id) {
                return BadRequest("Tiltaksgjennomføringen må ha samme tiltakstype som avtalen").left()
            }
        }

        tiltaksgjennomforinger.setAvtaleId(gjennomforingId, avtaleId)

        return Either.Right(Unit)
    }

    fun avbrytGjennomforing(gjennomforingId: UUID): StatusResponse<Unit> {
        val gjennomforing = tiltaksgjennomforinger.get(gjennomforingId)
            ?: return Either.Left(NotFound("Gjennomføringen finnes ikke"))

        if (gjennomforing.opphav == ArenaMigrering.Opphav.ARENA) {
            return Either.Left(BadRequest(message = "Gjennomføringen har opprinnelse fra Arena og kan ikke bli avbrutt i admin-flate."))
        }

        if (!gjennomforing.isAktiv()) {
            return Either.Left(BadRequest(message = "Gjennomføringen kan ikke avbrytes fordi den allerede er avsluttet."))
        }

        val antallDeltagere = deltakerRepository.getAll(gjennomforingId).size
        if (antallDeltagere > 0) {
            return Either.Left(BadRequest(message = "Gjennomføringen kan ikke avbrytes fordi den har $antallDeltagere deltager(e) koblet til seg."))
        }

        db.transaction { tx ->
            tiltaksgjennomforinger.setAvslutningsstatus(tx, gjennomforingId, Avslutningsstatus.AVBRUTT)
            val dto = tiltaksgjennomforinger.get(gjennomforingId, tx)!!
            tiltaksgjennomforingKafkaProducer.publish(TiltaksgjennomforingDto.from(dto))
        }

        return Either.Right(Unit)
    }

    private fun dispatchNotificationToNewAdministrators(
        tx: TransactionalSession,
        dbo: TiltaksgjennomforingDbo,
        navIdent: String,
    ) {
        val currentAdministratorer = get(dbo.id)?.administratorer?.map { it.navIdent }?.toSet() ?: setOf()

        val administratorsToNotify = (dbo.administratorer - currentAdministratorer - navIdent)
            .toNonEmptyListOrNull() ?: return

        val notification = ScheduledNotification(
            type = NotificationType.NOTIFICATION,
            title = "Du har blitt satt som administrator på gjennomføringen \"${dbo.navn}\"",
            targets = administratorsToNotify,
            createdAt = Instant.now(),
        )
        notificationRepository.insert(notification, tx)
    }
}
