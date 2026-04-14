package no.nav.mulighetsrommet.api.tiltakstype.api

import io.ktor.server.routing.RoutingContext
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeFeature

data class TiltakstypeFilter(
    val sortering: String? = null,
    val features: Set<TiltakstypeFeature> = setOf(),
)

fun RoutingContext.getTiltakstypeFilter(): TiltakstypeFilter {
    val sortering = call.request.queryParameters["sort"]
    return TiltakstypeFilter(
        sortering = sortering,
        features = setOf(TiltakstypeFeature.VISES_I_TILTAKSADMINISTRASJON),
    )
}
