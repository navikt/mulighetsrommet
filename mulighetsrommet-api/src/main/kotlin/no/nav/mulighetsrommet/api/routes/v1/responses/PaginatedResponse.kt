package no.nav.mulighetsrommet.api.routes.v1.responses

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.database.utils.Pagination
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
            val pageSize = pagination.pageSize ?: totalCount
            return PaginatedResponse(
                pagination = PaginationSummary(
                    totalCount = totalCount,
                    pageSize = pageSize,
                    totalPages = ceil((totalCount.toDouble() / pageSize)).toInt(),
                ),
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
)
