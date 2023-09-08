package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.domain.dto.TiltakstypeNokkeltallDto
import no.nav.mulighetsrommet.api.repositories.AvtaleRepository
import no.nav.mulighetsrommet.api.repositories.DeltakerRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.api.routes.v1.responses.PaginatedResponse
import no.nav.mulighetsrommet.api.routes.v1.responses.Pagination
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.api.utils.TiltakstypeFilter
import no.nav.mulighetsrommet.domain.dto.TiltakstypeDto
import java.util.*

class TiltakstypeService(
    private val tiltakstypeRepository: TiltakstypeRepository,
    private val tiltaksgjennomforingRepository: TiltaksgjennomforingRepository,
    private val avtaleRepository: AvtaleRepository,
    private val deltakerRepository: DeltakerRepository,
) {
    fun getWithFilter(
        tiltakstypeFilter: TiltakstypeFilter,
        paginationParams: PaginationParams,
    ): PaginatedResponse<TiltakstypeDto> {
        val (totalCount, items) = tiltakstypeRepository.getAllSkalMigreres(
            tiltakstypeFilter,
            paginationParams,
        )

        return PaginatedResponse(
            data = items,
            pagination = Pagination(
                totalCount = totalCount,
                currentPage = paginationParams.page,
                pageSize = paginationParams.limit,
            ),
        )
    }

    fun getById(id: UUID): TiltakstypeDto? {
        return tiltakstypeRepository.get(id)
    }

    fun getBySanityId(sanityId: UUID): TiltakstypeDto? {
        return tiltakstypeRepository.getBySanityId(sanityId)
    }

    fun getNokkeltallForTiltakstype(tiltakstypeId: UUID): TiltakstypeNokkeltallDto {
        val antallGjennomforinger = tiltaksgjennomforingRepository.countGjennomforingerForTiltakstypeWithId(tiltakstypeId)
        val antallAvtaler = avtaleRepository.countAktiveAvtalerForTiltakstypeWithId(tiltakstypeId)
        val antallDeltakere = deltakerRepository.countAntallDeltakereForTiltakstypeWithId(tiltakstypeId)
        return TiltakstypeNokkeltallDto(
            antallTiltaksgjennomforinger = antallGjennomforinger,
            antallAvtaler = antallAvtaler,
            antallDeltakere = antallDeltakere,
        )
    }
}
