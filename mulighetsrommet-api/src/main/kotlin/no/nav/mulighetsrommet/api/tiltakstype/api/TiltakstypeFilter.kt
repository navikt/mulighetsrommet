package no.nav.mulighetsrommet.api.tiltakstype.api

import io.ktor.server.routing.RoutingContext

data class TiltakstypeFilter(
    val sortering: String? = null,
)

fun RoutingContext.getTiltakstypeFilter(): TiltakstypeFilter {
    val sortering = call.request.queryParameters["sort"]
    return TiltakstypeFilter(
        sortering = sortering,
    )
}
