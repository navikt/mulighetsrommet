package no.nav.mulighetsrommet.api.services

import arrow.core.Either
import no.nav.mulighetsrommet.api.domain.dto.TiltaksgjennomforingNokkeltallDto
import no.nav.mulighetsrommet.api.repositories.AvtaleRepository
import no.nav.mulighetsrommet.api.repositories.DeltakerRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.routes.v1.TiltaksgjennomforingRequest
import no.nav.mulighetsrommet.api.routes.v1.responses.*
import no.nav.mulighetsrommet.api.utils.AdminTiltaksgjennomforingFilter
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingAdminDto
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingNotificationDto
import java.time.LocalDate
import java.util.*

class TiltaksgjennomforingService(
    private val tiltaksgjennomforingRepository: TiltaksgjennomforingRepository,
    private val deltakerRepository: DeltakerRepository,
    private val avtaleRepository: AvtaleRepository,
    private val sanityTiltaksgjennomforingService: SanityTiltaksgjennomforingService,
    private val virksomhetService: VirksomhetService,
    private val utkastService: UtkastService,
) {
    suspend fun upsert(
        request: TiltaksgjennomforingRequest,
        currentDate: LocalDate = LocalDate.now(),
    ): StatusResponse<TiltaksgjennomforingAdminDto> {
        val avtale = avtaleRepository.get(request.avtaleId)
            ?: return Either.Left(BadRequest("Avtalen finnes ikke"))

        if (avtale.sluttDato.isBefore(currentDate)) {
            return Either.Left(BadRequest("Avtalens sluttdato har passert"))
        }

        return request.toDbo()
            .onRight { virksomhetService.hentEnhet(it.arrangorOrganisasjonsnummer) }
            .onRight { tiltaksgjennomforingRepository.upsert(it) }
            .map { tiltaksgjennomforingRepository.get(request.id)!! }
            .onRight { sanityTiltaksgjennomforingService.opprettSanityTiltaksgjennomforing(it) }
            .onRight { utkastService.deleteUtkast(it.id) }
    }

    fun get(id: UUID): TiltaksgjennomforingAdminDto? =
        tiltaksgjennomforingRepository.get(id)

    fun getAll(
        pagination: PaginationParams,
        filter: AdminTiltaksgjennomforingFilter,
    ): PaginatedResponse<TiltaksgjennomforingAdminDto> =
        tiltaksgjennomforingRepository
            .getAll(pagination, filter)
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

    fun getBySanityIds(sanityIds: List<UUID>): Map<String, TiltaksgjennomforingAdminDto> {
        return tiltaksgjennomforingRepository.getBySanityIds(sanityIds)
    }

    fun delete(id: UUID, currentDate: LocalDate = LocalDate.now()): StatusResponse<Int> {
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

        return Either.Right(tiltaksgjennomforingRepository.delete(id))
    }

    fun getAllMidlertidigStengteGjennomforingerSomNarmerSegSluttdato(): List<TiltaksgjennomforingNotificationDto> {
        return tiltaksgjennomforingRepository.getAllMidlertidigStengteGjennomforingerSomNarmerSegSluttdato()
    }

    fun getLokasjonerForBrukersEnhet(enhetsId: String, fylkeId: String): List<String> {
        return tiltaksgjennomforingRepository.getLokasjonerForEnhet(enhetsId, fylkeId)
    }

    fun avbrytGjennomforing(gjennomforingId: UUID): StatusResponse<Int> {
        val gjennomforing = tiltaksgjennomforingRepository.get(gjennomforingId)
            ?: return Either.Left(NotFound("Fant ikke gjennomføringen med id $gjennomforingId"))

        if (gjennomforing.opphav == ArenaMigrering.Opphav.ARENA) {
            return Either.Left(BadRequest(message = "Gjennomføringen har opprinnelse fra Arena og kan ikke bli avbrutt i admin-flate."))
        }

        val antallDeltagere = deltakerRepository.getAll(gjennomforingId).size
        if (antallDeltagere > 0) {
            return Either.Left(BadRequest(message = "Gjennomføringen kan ikke avbrytes fordi den har $antallDeltagere deltager(e) koblet til seg."))
        }
        return Either.Right(tiltaksgjennomforingRepository.avbrytGjennomforing(gjennomforingId))
    }
}
