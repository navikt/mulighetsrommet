package no.nav.mulighetsrommet.api.utils

import io.ktor.server.application.*
import io.ktor.util.pipeline.*

class PaginationParams(
    page: Int?,
    limit: Int?
) {
    val page: Int
    private val size: Int
    val offset get() = (page - 1) * size
    val limit get() = size

    init {
        this.page = page ?: 1
        this.size = limit ?: 50
    }
}

fun <T : Any> PipelineContext<T, ApplicationCall>.getPaginationParams(): PaginationParams {
    return PaginationParams(
        page = call.parameters["page"]?.toIntOrNull(),
        limit = call.parameters["size"]?.toIntOrNull()
    )
}
