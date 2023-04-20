package no.nav.mulighetsrommet.api.routes.v1.responses

import kotlinx.serialization.Serializable

@Serializable
data class PaginatedResponse<T>(
    val pagination: Pagination,
    val data: List<T>,
)

@Serializable
data class Pagination(
    val totalCount: Int,
    val currentPage: Int,
    val pageSize: Int,
)
