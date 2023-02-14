package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.repositories.AvtaleRepository
import no.nav.mulighetsrommet.api.routes.v1.responses.PaginatedResponse
import no.nav.mulighetsrommet.api.routes.v1.responses.Pagination
import no.nav.mulighetsrommet.api.utils.AvtaleFilter
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.domain.dto.AvtaleAdminDto
import java.util.*

class AvtaleService(private val avtaler: AvtaleRepository) {

    fun get(id: UUID): AvtaleAdminDto? {
        return avtaler.get(id)
    }

    fun getAll(pagination: PaginationParams): PaginatedResponse<AvtaleAdminDto> {
        val (totalCount, items) = avtaler.getAll(pagination)

        return PaginatedResponse(
            data = items,
            pagination = Pagination(
                totalCount = totalCount,
                currentPage = pagination.page,
                pageSize = pagination.limit
            )
        )
    }

    fun getAvtalerForTiltakstype(tiltakstypeId: UUID, filter: AvtaleFilter, pagination: PaginationParams): PaginatedResponse<AvtaleAdminDto> {
        val (totalCount, items) = avtaler.getAvtalerForTiltakstype(tiltakstypeId, filter, pagination)

        return PaginatedResponse(
            data = items,
            pagination = Pagination(
                totalCount = totalCount,
                currentPage = pagination.page,
                pageSize = pagination.limit
            )
        )
    }
}
