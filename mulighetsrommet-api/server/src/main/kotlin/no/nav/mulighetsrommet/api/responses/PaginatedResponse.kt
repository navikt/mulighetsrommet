package no.nav.mulighetsrommet.api.responses

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.shared.Pagination
import kotlin.math.ceil

@Serializable
data class PaginatedResponse<T>(
    val pagination: PaginationSummary,
    val data: List<T>,
) {

    companion object {
        /**
         * Utility to wrap the [data] in a [PaginatedResponse] with a default [PaginationSummary] derived from [data].
         */
        fun <T> of(data: List<T>): PaginatedResponse<T> = PaginatedResponse(
            pagination = PaginationSummary(
                totalCount = data.size,
                totalPages = 1,
                pageSize = data.size,
            ),
            data = data,
        )

        fun <T> of(pagination: Pagination, totalCount: Int, data: List<T>): PaginatedResponse<T> {
            return PaginatedResponse(
                pagination = PaginationSummary.of(pagination, totalCount),
                data = data,
            )
        }
    }
}

@Serializable
data class PaginationSummary(
    val totalCount: Int,
    val pageSize: Int,
    val totalPages: Int,
) {
    companion object {
        fun of(pagination: Pagination, totalCount: Int): PaginationSummary {
            val pageSize = pagination.pageSize ?: totalCount
            val totalPages = if (pageSize == 0) 0 else ceil(totalCount.toDouble() / pageSize).toInt()

            return PaginationSummary(
                totalCount = totalCount,
                pageSize = pageSize,
                totalPages = totalPages,
            )
        }
    }
}
