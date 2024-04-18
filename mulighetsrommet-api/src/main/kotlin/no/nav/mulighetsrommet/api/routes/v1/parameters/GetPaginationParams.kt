package no.nav.mulighetsrommet.api.routes.v1.parameters

import io.ktor.server.application.*
import io.ktor.util.pipeline.*
import no.nav.mulighetsrommet.database.utils.Pagination

const val FIRST_PAGE = 1

const val DEFAULT_PAGE_SIZE = 50

fun <T : Any> PipelineContext<T, ApplicationCall>.getPaginationParams(): Pagination {
    return Pagination.of(
        page = call.parameters["page"]?.toIntOrNull() ?: FIRST_PAGE,
        size = call.parameters["size"]?.toIntOrNull() ?: DEFAULT_PAGE_SIZE,
    )
}
