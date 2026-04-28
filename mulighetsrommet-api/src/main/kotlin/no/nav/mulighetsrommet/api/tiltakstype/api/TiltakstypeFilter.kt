package no.nav.mulighetsrommet.api.tiltakstype.api

import io.ktor.server.routing.RoutingContext

enum class TiltakstypeSortField {
    NAVN,
    TILTAKSKODE,
}

enum class SortDirection {
    ASC,
    DESC,
}

data class TiltakstypeFilter(
    val sortField: TiltakstypeSortField = TiltakstypeSortField.NAVN,
    val sortDirection: SortDirection = SortDirection.ASC,
)

fun RoutingContext.getTiltakstypeFilter(): TiltakstypeFilter {
    val sortField = call.request.queryParameters["sortField"]
        ?.let { runCatching { TiltakstypeSortField.valueOf(it) }.getOrNull() }
        ?: TiltakstypeSortField.NAVN
    val sortDirection = call.request.queryParameters["sortDirection"]
        ?.let { runCatching { SortDirection.valueOf(it) }.getOrNull() }
        ?: SortDirection.ASC
    return TiltakstypeFilter(sortField = sortField, sortDirection = sortDirection)
}
