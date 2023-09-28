package no.nav.mulighetsrommet.api.services

import arrow.core.Either
import kotliquery.Session
import no.nav.mulighetsrommet.api.avtaler.AvtaleRequestValidator
import no.nav.mulighetsrommet.api.domain.dto.AvtaleNokkeltallDto
import no.nav.mulighetsrommet.api.repositories.AvtaleRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.UtkastRepository
import no.nav.mulighetsrommet.api.routes.v1.AvtaleRequest
import no.nav.mulighetsrommet.api.routes.v1.responses.*
import no.nav.mulighetsrommet.api.utils.AvtaleFilter
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering.Opphav
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dto.AvtaleAdminDto
import no.nav.mulighetsrommet.domain.dto.AvtaleNotificationDto
import no.nav.mulighetsrommet.domain.dto.Avtalestatus
import no.nav.mulighetsrommet.notifications.NotificationRepository
import no.nav.mulighetsrommet.notifications.NotificationType
import no.nav.mulighetsrommet.notifications.ScheduledNotification
import java.time.Instant
import java.time.LocalDate
import java.util.*

class AvtaleService(
    private val avtaler: AvtaleRepository,
    private val tiltaksgjennomforinger: TiltaksgjennomforingRepository,
    private val virksomhetService: VirksomhetService,
    private val notificationRepository: NotificationRepository,
    private val utkastRepository: UtkastRepository,
    private val validator: AvtaleRequestValidator,
    private val db: Database,
) {
    fun get(id: UUID): AvtaleAdminDto? {
        return avtaler.get(id)
    }

    suspend fun upsert(request: AvtaleRequest, navIdent: String): Either<List<ValidationError>, AvtaleAdminDto> {
        virksomhetService.getOrSyncVirksomhet(request.leverandorOrganisasjonsnummer)

        val currentAvtale = get(request.id)
        return validator.validate(request)
            .map { it.toDbo() }
            .map { dbo ->
                db.transaction { tx ->
                    avtaler.upsert(dbo, tx)
                    utkastRepository.delete(dbo.id, tx)

                    val currentAdministrator = currentAvtale?.administrator?.navIdent
                    if (navIdent != request.administrator && currentAdministrator != request.administrator) {
                        dispatchSattSomAdministratorNofication(dbo.navn, request.administrator, tx)
                    }
                    avtaler.get(dbo.id, tx)!!
                }
            }
    }

    fun delete(id: UUID, currentDate: LocalDate = LocalDate.now()): StatusResponse<Unit> {
        val avtale = avtaler.get(id)
            ?: return Either.Left(NotFound("Fant ikke avtale for sletting"))

        if (avtale.opphav == Opphav.ARENA) {
            return Either.Left(BadRequest(message = "Avtalen har opprinnelse fra Arena og kan ikke bli slettet i admin-flate."))
        }

        if (avtale.startDato <= currentDate) {
            return Either.Left(BadRequest(message = "Avtalen er aktiv og kan derfor ikke slettes."))
        }

        val gjennomforingerForAvtale = tiltaksgjennomforinger.getAll(avtaleId = id)

        if (gjennomforingerForAvtale.first > 0) {
            return Either.Left(BadRequest(message = "Avtalen har ${gjennomforingerForAvtale.first} ${if (gjennomforingerForAvtale.first > 1) "tiltaksgjennomføringer" else "tiltaksgjennomføring"} koblet til seg. Du må frikoble ${if (gjennomforingerForAvtale.first > 1) "gjennomføringene" else "gjennomføringen"} før du kan slette avtalen."))
        }

        return Either.Right(avtaler.delete(id))
    }

    fun getAll(
        filter: AvtaleFilter,
        pagination: PaginationParams = PaginationParams(),
    ): PaginatedResponse<AvtaleAdminDto> {
        val (totalCount, items) = avtaler.getAll(filter, pagination)

        return PaginatedResponse(
            data = items,
            pagination = Pagination(
                totalCount = totalCount,
                currentPage = pagination.page,
                pageSize = pagination.limit,
            ),
        )
    }

    fun getNokkeltallForAvtaleMedId(id: UUID): AvtaleNokkeltallDto {
        val antallTiltaksgjennomforinger = avtaler.countTiltaksgjennomforingerForAvtaleWithId(id)
        val antallDeltakereForAvtale = tiltaksgjennomforinger.countDeltakereForAvtaleWithId(id)
        return AvtaleNokkeltallDto(
            antallTiltaksgjennomforinger = antallTiltaksgjennomforinger,
            antallDeltakere = antallDeltakereForAvtale,
        )
    }

    fun getAllAvtalerSomNarmerSegSluttdato(): List<AvtaleNotificationDto> {
        return avtaler.getAllAvtalerSomNarmerSegSluttdato()
    }

    fun avbrytAvtale(avtaleId: UUID): StatusResponse<Unit> {
        val avtaleForAvbryting = avtaler.get(avtaleId)
            ?: return Either.Left(NotFound("Fant ikke avtale for avbrytelse med id '$avtaleId'"))

        if (avtaleForAvbryting.opphav == Opphav.ARENA) {
            return Either.Left(BadRequest(message = "Avtalen har opprinnelse fra Arena og kan ikke bli avbrutt fra admin-flate."))
        }

        if (avtaleForAvbryting.avtalestatus === Avtalestatus.Avsluttet) {
            return Either.Left(BadRequest(message = "Avtalen er allerede avsluttet og kan derfor ikke avbrytes."))
        }

        val gjennomforingerForAvtale = tiltaksgjennomforinger.getAll(avtaleId = avtaleId)

        if (gjennomforingerForAvtale.first > 0) {
            return Either.Left(BadRequest(message = "Avtalen har ${gjennomforingerForAvtale.first} ${if (gjennomforingerForAvtale.first > 1) "tiltaksgjennomføringer" else "tiltaksgjennomføring"} koblet til seg. Du må frikoble ${if (gjennomforingerForAvtale.first > 1) "gjennomføringene" else "gjennomføringen"} før du kan avbryte avtalen."))
        }

        return Either.Right(avtaler.setAvslutningsstatus(avtaleId, Avslutningsstatus.AVBRUTT))
    }

    private fun dispatchSattSomAdministratorNofication(avtaleNavn: String, administrator: String, tx: Session) {
        val notification = ScheduledNotification(
            type = NotificationType.NOTIFICATION,
            title = "Du har blitt satt som administrator på avtalen \"$avtaleNavn\"",
            targets = listOf(administrator),
            createdAt = Instant.now(),
        )
        notificationRepository.insert(notification, tx)
    }
}
