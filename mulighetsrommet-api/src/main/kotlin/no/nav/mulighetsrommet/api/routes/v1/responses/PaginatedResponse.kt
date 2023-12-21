package no.nav.mulighetsrommet.api.routes.v1.responses

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.utils.PaginationParams
import kotlin.math.ceil

@Serializable
data class PaginatedResponse<T>(
    val pagination: Pagination,
    val data: List<T>,
) {

    companion object {
        /**
         * Utility to wrap the [data] in a [PaginatedResponse] with a default [Pagination] derived from [data].
         */
        fun <T> of(data: List<T>): PaginatedResponse<T> = PaginatedResponse(
            pagination = Pagination(
                totalCount = data.size,
                totalPages = 1,
                currentPage = 1,
                pageSize = data.size,
            ),
            data = data,
        )

        fun <T> of(pagination: PaginationParams, totalCount: Int, data: List<T>): PaginatedResponse<T> =
            PaginatedResponse(
                pagination = Pagination(
                    totalCount = totalCount,
                    currentPage = pagination.page,
                    pageSize = pagination.limit,
                    totalPages = ceil((totalCount.toDouble() / pagination.limit)).toInt(),
                ),
                data = data,
            )
    }
}

@Serializable
data class Pagination(
    val totalCount: Int,
    val currentPage: Int,
    val pageSize: Int,
    val totalPages: Int,
)
