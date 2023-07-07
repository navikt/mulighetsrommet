package no.nav.mulighetsrommet.api.services

import arrow.core.Either
import arrow.core.flatMap
import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.api.domain.dto.TiltaksgjennomforingNokkeltallDto
import no.nav.mulighetsrommet.api.repositories.AvtaleRepository
import no.nav.mulighetsrommet.api.repositories.DeltakerRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.routes.v1.responses.*
import no.nav.mulighetsrommet.api.utils.AdminTiltaksgjennomforingFilter
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.utils.DatabaseOperationError
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.getOrThrow
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingAdminDto
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingNotificationDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

class TiltaksgjennomforingService(
    private val tiltaksgjennomforingRepository: TiltaksgjennomforingRepository,
    private val deltakerRepository: DeltakerRepository,
    private val avtaleRepository: AvtaleRepository,
    private val sanityTiltaksgjennomforingService: SanityTiltaksgjennomforingService,
    private val virksomhetService: VirksomhetService,
) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    suspend fun upsert(dbo: TiltaksgjennomforingDbo, currentDate: LocalDate = LocalDate.now()): StatusResponse<TiltaksgjennomforingAdminDto> {
        if (dbo.avtaleId == null) {
            return Either.Left(BadRequest("Avtale id kan ikke være null"))
        }
        val avtale = avtaleRepository.get(dbo.avtaleId!!).getOrThrow()
            ?: return Either.Left(BadRequest("Avtalen finnes ikke"))

        if (avtale.sluttDato.isBefore(currentDate)) {
            return Either.Left(BadRequest("Avtalens sluttdato har passert"))
        }

        virksomhetService.hentEnhet(dbo.arrangorOrganisasjonsnummer)
        return tiltaksgjennomforingRepository.upsert(dbo)
            .flatMap { tiltaksgjennomforingRepository.get(dbo.id) }
            .map { it!! }
            .mapLeft { ServerError("Feil ved upsert av tiltaksgjennomføring") }
            .onRight { sanityTiltaksgjennomforingService.opprettSanityTiltaksgjennomforing(it) }
    }

    fun get(id: UUID): QueryResult<TiltaksgjennomforingAdminDto?> =
        tiltaksgjennomforingRepository.get(id)
            .onLeft { log.error("Klarte ikke hente tiltaksgjennomføring", it.error) }

    fun getAll(
        pagination: PaginationParams,
        filter: AdminTiltaksgjennomforingFilter,
    ): Either<DatabaseOperationError, PaginatedResponse<TiltaksgjennomforingAdminDto>> =
        tiltaksgjennomforingRepository
            .getAll(pagination, filter)
            .onLeft { log.error("Klarte ikke hente tiltaksgjennomføringer", it.error) }
            .map { (totalCount, data) ->
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
        val gjennomforing = tiltaksgjennomforingRepository.get(id).getOrNull()
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

        return tiltaksgjennomforingRepository
            .delete(id)
            .mapLeft {
                ServerError(message = "Det oppsto en feil ved sletting av gjennomføringen")
            }
    }

    fun getAllMidlertidigStengteGjennomforingerSomNarmerSegSluttdato(): List<TiltaksgjennomforingNotificationDto> {
        return tiltaksgjennomforingRepository.getAllMidlertidigStengteGjennomforingerSomNarmerSegSluttdato()
    }

    fun getLokasjonerForBrukersEnhet(enhetsId: String, fylkeId: String): List<String> {
        return tiltaksgjennomforingRepository.getLokasjonerForEnhet(enhetsId, fylkeId)
    }
}
