package no.nav.mulighetsrommet.api.navansatt.ktor

import io.ktor.server.routing.Route
import io.ktor.server.routing.RouteSelector
import io.ktor.server.routing.RouteSelectorEvaluation
import io.ktor.server.routing.RoutingResolveContext
import io.ktor.util.AttributeKey
import no.nav.mulighetsrommet.api.navansatt.model.Rolle

fun Route.authorize(
    requiredRole: Rolle? = null,
    anyOf: Set<Rolle> = emptySet(),
    allOf: Set<Rolle> = emptySet(),
    build: Route.() -> Unit,
): Route {
    val roller = buildList {
        allOf.forEach { add(AnyRoles(setOf(it))) }
        if (requiredRole != null) add(AnyRoles(setOf(requiredRole)))
        if (anyOf.isNotEmpty()) add(AnyRoles(anyOf))
    }
    val authorizedRoute = createChild(NavAnsattAuthorizationRouteSelector(roller = RequiredRoles(roller)))

    val parentRoles = generateSequence(authorizedRoute) { it.parent }
        .mapNotNull { it.attributes.getOrNull(AuthorizedRolesKey) }
        .map { it.roles }
        .flatten()
        .toList()
        .reversed()
        .distinct()

    val combinedRoles = RequiredRoles(parentRoles + roller)

    authorizedRoute.attributes.put(AuthorizedRolesKey, combinedRoles)

    authorizedRoute.install(NavAnsattAuthorizationPlugin) {
        requiredRoles = combinedRoles
    }

    authorizedRoute.build()
    return authorizedRoute
}

private val AuthorizedRolesKey = AttributeKey<RequiredRoles>("AuthorizedRolesKey")

class NavAnsattAuthorizationRouteSelector(val roller: RequiredRoles) : RouteSelector() {
    override suspend fun evaluate(context: RoutingResolveContext, segmentIndex: Int): RouteSelectorEvaluation {
        return RouteSelectorEvaluation.Transparent
    }

    override fun toString(): String = "(authorize $roller)"
}
