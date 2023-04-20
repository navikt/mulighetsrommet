package no.nav.mulighetsrommet.api.utils

import io.ktor.server.application.*
import io.ktor.util.pipeline.*

class PaginationParams(
    private val nullablePage: Int? = null,
    private val nullableLimit: Int? = null,
) {
    val page get() = nullablePage ?: 1
    val limit get() = nullableLimit ?: DEFAULT_PAGINATION_LIMIT
    val offset get() = (page - 1) * limit
}

const val DEFAULT_PAGINATION_LIMIT = 50

fun <T : Any> PipelineContext<T, ApplicationCall>.getPaginationParams(): PaginationParams {
    return PaginationParams(
        nullablePage = call.parameters["page"]?.toIntOrNull(),
        nullableLimit = call.parameters["size"]?.toIntOrNull(),
    )
}
