package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.domain.dto.TiltakstypeNokkeltallDto
import no.nav.mulighetsrommet.api.repositories.AvtaleRepository
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
    private val avtaleRepository: AvtaleRepository
) {
    fun getAll(
        paginationParams: PaginationParams
    ): PaginatedResponse<TiltakstypeDto> {
        val (totalCount, items) = tiltakstypeRepository.getAll(
            paginationParams
        )

        return PaginatedResponse(
            data = items,
            pagination = Pagination(
                totalCount = totalCount, currentPage = paginationParams.page, pageSize = paginationParams.limit
            )
        )
    }

    fun getWithFilter(
        tiltakstypeFilter: TiltakstypeFilter,
        paginationParams: PaginationParams
    ): PaginatedResponse<TiltakstypeDto> {
        val (totalCount, items) = tiltakstypeRepository.getAll(
            tiltakstypeFilter, paginationParams
        )

        return PaginatedResponse(
            data = items,
            pagination = Pagination(
                totalCount = totalCount, currentPage = paginationParams.page, pageSize = paginationParams.limit
            )
        )
    }

    fun getById(id: UUID): TiltakstypeDto? {
        return tiltakstypeRepository.get(id)
    }

    fun getNokkeltallForTiltakstype(id: UUID): TiltakstypeNokkeltallDto {
        val antallGjennomforinger = tiltaksgjennomforingRepository.countGjennomforingerForTiltakstypeWithId(id)
        val antallAvtaler = avtaleRepository.countAktiveAvtalerForTiltakstypeWithId(id)
        return TiltakstypeNokkeltallDto(antallTiltaksgjennomforinger = antallGjennomforinger, antallAvtaler = antallAvtaler)
    }
}
