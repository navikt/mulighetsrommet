package no.nav.mulighetsrommet.api.parameters

import io.ktor.server.routing.RoutingContext
import no.nav.mulighetsrommet.database.utils.Pagination

const val FIRST_PAGE = 1

const val DEFAULT_PAGE_SIZE = 50

fun RoutingContext.getPaginationParams(page: Int? = FIRST_PAGE, size: Int? = DEFAULT_PAGE_SIZE): Pagination {
    return Pagination.of(
        page = call.parameters["page"]?.toIntOrNull() ?: FIRST_PAGE,
        size = call.parameters["size"]?.toIntOrNull() ?: DEFAULT_PAGE_SIZE,
    )
}
