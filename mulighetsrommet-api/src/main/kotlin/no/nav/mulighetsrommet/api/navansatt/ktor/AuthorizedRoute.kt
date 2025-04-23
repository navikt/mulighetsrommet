package no.nav.mulighetsrommet.api.navansatt.ktor

import io.ktor.server.routing.*
import io.ktor.util.*
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsattRolle
import no.nav.mulighetsrommet.api.navansatt.model.Rolle

fun Route.authorize(
    requiredRole: Rolle,
    vararg additionalRoles: Rolle,
    build: Route.() -> Unit,
): Route {
    val routeRoles = listOf(requiredRole, *additionalRoles)

    val authorizedRoute = createChild(NavAnsattAuthorizationRouteSelector(routeRoles))

    val parentRoles = generateSequence(authorizedRoute) { it.parent }
        .mapNotNull { it.attributes.getOrNull(AuthorizedRolesKey) }
        .flatten()
        .toList()
        .reversed()
        .distinct()

    val combinedRoles = parentRoles + routeRoles.map { NavAnsattRolle.Companion.generell(it) }

    authorizedRoute.attributes.put(AuthorizedRolesKey, combinedRoles)

    authorizedRoute.install(NavAnsattAuthorizationPlugin) {
        roles = combinedRoles
    }

    authorizedRoute.build()
    return authorizedRoute
}

private val AuthorizedRolesKey = AttributeKey<List<NavAnsattRolle>>("AuthorizedRolesKey")

private class NavAnsattAuthorizationRouteSelector(val roles: List<Rolle>) : RouteSelector() {
    override suspend fun evaluate(context: RoutingResolveContext, segmentIndex: Int): RouteSelectorEvaluation {
        return RouteSelectorEvaluation.Companion.Transparent
    }

    override fun toString(): String = "(authorize ${roles.joinToString { it.name }})"
}
