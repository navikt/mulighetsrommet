package no.nav.mulighetsrommet.api.utils

import io.ktor.server.application.*
import io.ktor.util.pipeline.*

class PaginationParams(
    val nullablePage: Int?,
    val nullableLimit: Int?
) {
    val page get() = nullablePage ?: 1
    val limit get() = nullableLimit ?: 50
    val offset get() = (page - 1) * limit
}

fun <T : Any> PipelineContext<T, ApplicationCall>.getPaginationParams(): PaginationParams {
    return PaginationParams(
        nullablePage = call.parameters["page"]?.toIntOrNull(),
        nullableLimit = call.parameters["size"]?.toIntOrNull()
    )
}
