package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.api.routes.v1.responses.PaginatedResponse
import no.nav.mulighetsrommet.api.routes.v1.responses.Pagination
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.api.utils.TiltakstypeFilter
import no.nav.mulighetsrommet.domain.dto.TiltakstypeAdminDto
import java.util.*

class TiltakstypeService(private val tiltakstypeRepository: TiltakstypeRepository) {
    fun getWithFilter(
        tiltakstypeFilter: TiltakstypeFilter,
        paginationParams: PaginationParams
    ): PaginatedResponse<TiltakstypeAdminDto> {
        val (totalCount, items) = tiltakstypeRepository.getAll(
            tiltakstypeFilter,
            paginationParams
        )

        return PaginatedResponse(
            data = items,
            pagination = Pagination(
                totalCount = totalCount,
                currentPage = paginationParams.page,
                pageSize = paginationParams.limit
            )
        )
    }

    fun getById(id: UUID): TiltakstypeAdminDto? {
        return tiltakstypeRepository.getForAdmin(id)
    }

    fun lagreTags(tags: Set<String>, id: UUID): TiltakstypeAdminDto? {
        return tiltakstypeRepository.lagreTagsForTiltakstype(tags, id)
    }

    fun getAlleTagsForTiltakstype(): Set<String> {
        return tiltakstypeRepository.getAlleTagsForAlleTiltakstyper()
    }
}
