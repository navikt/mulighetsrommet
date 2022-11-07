package no.nav.mulighetsrommet.api.routes.v1.responses

import kotlinx.serialization.Serializable

interface ListResponse {
    val data: List<Any>
    val pagination: Pagination?
    val links: Links?
}

@Serializable
data class Pagination(
    val total_count: Int,
    val current_page: Int,
    val total_pages: Int,
    val _links: Links
)

@Serializable
data class Links(
    val previous: Int? = null,
    val next: Int? = null
)
