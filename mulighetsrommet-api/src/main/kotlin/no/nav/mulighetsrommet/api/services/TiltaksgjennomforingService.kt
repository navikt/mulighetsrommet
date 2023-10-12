package no.nav.mulighetsrommet.api.services

import arrow.core.Either
import arrow.core.right
import io.ktor.server.plugins.*
import kotliquery.Session
import no.nav.mulighetsrommet.api.clients.vedtak.Innsatsgruppe
import no.nav.mulighetsrommet.api.domain.dto.TiltaksgjennomforingNokkeltallDto
import no.nav.mulighetsrommet.api.domain.dto.VeilederflateTiltaksgjennomforing
import no.nav.mulighetsrommet.api.repositories.AvtaleRepository
import no.nav.mulighetsrommet.api.repositories.DeltakerRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.UtkastRepository
import no.nav.mulighetsrommet.api.routes.v1.TiltaksgjennomforingRequest
import no.nav.mulighetsrommet.api.routes.v1.responses.*
import no.nav.mulighetsrommet.api.utils.AdminTiltaksgjennomforingFilter
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingAdminDto
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingDto
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingNotificationDto
import no.nav.mulighetsrommet.kafka.producers.TiltaksgjennomforingKafkaProducer
import no.nav.mulighetsrommet.notifications.NotificationRepository
import no.nav.mulighetsrommet.notifications.NotificationType
import no.nav.mulighetsrommet.notifications.ScheduledNotification
import java.time.Instant
import java.time.LocalDate
import java.util.*

class TiltaksgjennomforingService(
    private val tiltaksgjennomforingRepository: TiltaksgjennomforingRepository,
    private val deltakerRepository: DeltakerRepository,
    private val avtaleRepository: AvtaleRepository,
    private val virksomhetService: VirksomhetService,
    private val utkastRepository: UtkastRepository,
    private val tiltaksgjennomforingKafkaProducer: TiltaksgjennomforingKafkaProducer,
    private val notificationRepository: NotificationRepository,
    private val sanityTiltaksgjennomforingService: SanityTiltaksgjennomforingService,
    private val db: Database,
) {
    suspend fun upsert(
        request: TiltaksgjennomforingRequest,
        navIdent: String,
        currentDate: LocalDate = LocalDate.now(),
    ): StatusResponse<TiltaksgjennomforingAdminDto> {
        val avtale = avtaleRepository.get(request.avtaleId)
            ?: return Either.Left(BadRequest("Avtalen finnes ikke"))

        if (avtale.sluttDato.isBefore(currentDate)) {
            return Either.Left(BadRequest("Avtalens sluttdato har passert"))
        }
        virksomhetService.getOrSyncVirksomhet(request.arrangorOrganisasjonsnummer)

        val prevAdministrator = tiltaksgjennomforingRepository.get(request.id)?.administrator?.navIdent

        return request.toDbo()
            .map { dbo ->
                db.transactionSuspend { tx ->
                    tiltaksgjennomforingRepository.upsert(dbo, tx)
                    utkastRepository.delete(dbo.id, tx)
                    if (navIdent != request.administrator && request.administrator != prevAdministrator) {
                        dispatchSattSomAdministratorNotification(dbo.navn, request.administrator, tx)
                    }

                    val dto = tiltaksgjennomforingRepository.get(request.id, tx)!!

                    sanityTiltaksgjennomforingService.createOrPatchSanityTiltaksgjennomforing(dto, tx)
                    tiltaksgjennomforingKafkaProducer.publish(TiltaksgjennomforingDto.from(dto))
                    dto
                }
            }
    }

    fun get(id: UUID): TiltaksgjennomforingAdminDto? =
        tiltaksgjennomforingRepository.get(id)

    fun getAllSkalMigreres(
        pagination: PaginationParams,
        filter: AdminTiltaksgjennomforingFilter,
    ): PaginatedResponse<TiltaksgjennomforingAdminDto> =
        tiltaksgjennomforingRepository
            .getAll(
                pagination,
                search = filter.search,
                navEnhet = filter.navEnhet,
                tiltakstypeId = filter.tiltakstypeId,
                status = filter.status,
                sortering = filter.sortering,
                sluttDatoCutoff = filter.sluttDatoCutoff,
                dagensDato = filter.dagensDato,
                navRegion = filter.navRegion,
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
        sanityTiltakstypeIds: List<UUID>,
        innsatsgrupper: List<Innsatsgruppe>,
    ): List<VeilederflateTiltaksgjennomforing> =
        tiltaksgjennomforingRepository.getAllVeilederflateTiltaksgjennomforing(search, sanityTiltakstypeIds, innsatsgrupper)

    fun getAll(
        pagination: PaginationParams,
        filter: AdminTiltaksgjennomforingFilter,
    ): PaginatedResponse<TiltaksgjennomforingAdminDto> =
        tiltaksgjennomforingRepository
            .getAll(
                pagination,
                search = filter.search,
                navEnhet = filter.navEnhet,
                tiltakstypeId = filter.tiltakstypeId,
                status = filter.status,
                sortering = filter.sortering,
                sluttDatoCutoff = filter.sluttDatoCutoff,
                dagensDato = filter.dagensDato,
                navRegion = filter.navRegion,
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

    fun getNokkeltallForTiltaksgjennomforing(tiltaksgjennomforingId: UUID): TiltaksgjennomforingNokkeltallDto =
        TiltaksgjennomforingNokkeltallDto(
            antallDeltakere = deltakerRepository.countAntallDeltakereForTiltakstypeWithId(tiltaksgjennomforingId),
        )

    fun getAllGjennomforingerSomNarmerSegSluttdato(): List<TiltaksgjennomforingNotificationDto> {
        return tiltaksgjennomforingRepository.getAllGjennomforingerSomNarmerSegSluttdato()
    }

    fun kobleGjennomforingTilAvtale(gjennomforingId: UUID, avtaleId: UUID? = null) {
        return tiltaksgjennomforingRepository.updateAvtaleIdForGjennomforing(gjennomforingId, avtaleId)
    }

    fun getBySanityId(sanityId: UUID): TiltaksgjennomforingAdminDto? {
        return tiltaksgjennomforingRepository.getBySanityId(sanityId)
    }

    fun getBySanityIds(sanityIds: List<UUID>): Map<UUID, TiltaksgjennomforingAdminDto> {
        return tiltaksgjennomforingRepository.getBySanityIds(sanityIds)
    }

    suspend fun delete(id: UUID, currentDate: LocalDate = LocalDate.now()): StatusResponse<Unit> {
        val gjennomforing = tiltaksgjennomforingRepository.get(id)
            ?: return Either.Left(NotFound("Fant ikke gjennomføringen med id $id"))

        if (gjennomforing.opphav == ArenaMigrering.Opphav.ARENA) {
            return Either.Left(BadRequest(message = "Gjennomføringen har opprinnelse fra Arena og kan ikke bli slettet i admin-flate."))
        }

        if (gjennomforing.startDato <= currentDate) {
            return Either.Left(BadRequest(message = "Gjennomføringen er aktiv og kan derfor ikke slettes."))
        }

        val antallDeltagere = deltakerRepository.getAll(id).size
        if (antallDeltagere > 0) {
            return Either.Left(BadRequest(message = "Gjennomføringen kan ikke slettes fordi den har $antallDeltagere deltager(e) koblet til seg."))
        }

        val sanityId = gjennomforing.sanityId

        return db.transactionSuspend { tx ->
            tiltaksgjennomforingRepository.delete(id, tx)
            if (sanityId != null) {
                sanityTiltaksgjennomforingService.deleteSanityTiltaksgjennomforing(sanityId)
            }
            tiltaksgjennomforingKafkaProducer.retract(id)
        }.right()
    }

    fun getAllMidlertidigStengteGjennomforingerSomNarmerSegSluttdato(): List<TiltaksgjennomforingNotificationDto> {
        return tiltaksgjennomforingRepository.getAllMidlertidigStengteGjennomforingerSomNarmerSegSluttdato()
    }

    fun avbrytGjennomforing(gjennomforingId: UUID): StatusResponse<Unit> {
        val gjennomforing = tiltaksgjennomforingRepository.get(gjennomforingId)
            ?: return Either.Left(NotFound("Fant ikke gjennomføringen med id $gjennomforingId"))

        if (gjennomforing.opphav == ArenaMigrering.Opphav.ARENA) {
            return Either.Left(BadRequest(message = "Gjennomføringen har opprinnelse fra Arena og kan ikke bli avbrutt i admin-flate."))
        }

        val antallDeltagere = deltakerRepository.getAll(gjennomforingId).size
        if (antallDeltagere > 0) {
            return Either.Left(BadRequest(message = "Gjennomføringen kan ikke avbrytes fordi den har $antallDeltagere deltager(e) koblet til seg."))
        }

        return db.transaction { tx ->
            tiltaksgjennomforingRepository.avbrytGjennomforing(gjennomforingId, tx)
            val dto = tiltaksgjennomforingRepository.get(gjennomforingId, tx)!!
            tiltaksgjennomforingKafkaProducer.publish(TiltaksgjennomforingDto.from(dto))
        }.right()
    }

    private fun dispatchSattSomAdministratorNotification(
        gjennomforingNavn: String,
        administrator: String,
        tx: Session,
    ) {
        val notification = ScheduledNotification(
            type = NotificationType.NOTIFICATION,
            title = "Du har blitt satt som administrator på gjennomføringen \"${gjennomforingNavn}\"",
            targets = listOf(administrator),
            createdAt = Instant.now(),
        )
        notificationRepository.insert(notification, tx)
    }

    fun setTilgjengeligForVeileder(id: UUID, tilgjengeligForVeileder: Boolean) {
        val updatedRows = tiltaksgjennomforingRepository.setTilgjengeligForVeileder(id, tilgjengeligForVeileder)
        if (updatedRows != 1) throw NotFoundException("Fant ingen gjennomføring med id: $id")
    }
}
