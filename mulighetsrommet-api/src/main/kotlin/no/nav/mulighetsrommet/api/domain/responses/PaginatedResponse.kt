package no.nav.mulighetsrommet.api.domain.responses

import kotlinx.serialization.Serializable

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
            pagination = Pagination(totalCount = data.size, currentPage = 1, pageSize = data.size),
            data = data,
        )
    }
}

@Serializable
data class Pagination(
    val totalCount: Int,
    val currentPage: Int,
    val pageSize: Int,
)
