package no.nav.mulighetsrommet.api.routes.v1.responses

import kotlinx.serialization.Serializable

interface ListResponse {
    val data: List<Any>
    val pagination: Pagination?
}

@Serializable
data class Pagination(
    val totalCount: Int,
    val currentPage: Int,
    val pageSizeLimit: Int
)
