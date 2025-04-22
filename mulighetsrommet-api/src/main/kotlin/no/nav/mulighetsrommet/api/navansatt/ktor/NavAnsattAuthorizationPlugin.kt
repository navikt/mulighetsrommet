package no.nav.mulighetsrommet.api.navansatt.ktor

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.auth.AuthenticationChecked
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RouteSelector
import io.ktor.server.routing.RouteSelectorEvaluation
import io.ktor.server.routing.RoutingResolveContext
import io.ktor.util.AttributeKey
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsattRolle
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.navansatt.service.NavAnsattPrincipal

class NavAnsattAuthorizationPluginConfiguration {
    var roles: List<NavAnsattRolle> = listOf()
}

val NavAnsattAuthorizationPlugin = createRouteScopedPlugin(
    name = "NavAnsattAuthorizationPlugin",
    createConfiguration = ::NavAnsattAuthorizationPluginConfiguration,
) {
    val requiredRoles = pluginConfig.roles

    require(requiredRoles.isNotEmpty()) { "Minst én rolle er påkrevd" }

    pluginConfig.apply {
        on(AuthenticationChecked) { call ->
            val principal = call.principal<NavAnsattPrincipal>()

            if (principal == null) {
                return@on call.respond(HttpStatusCode.Companion.Unauthorized)
            }

            val navAnsattRoles = principal.roller

            if (navAnsattRoles.isEmpty()) {
                return@on call.respond(
                    HttpStatusCode.Companion.Forbidden,
                    "Mangler roller minst én av følgende roller: ${requiredRoles.map { it.rolle }.joinToString(", ")}",
                )
            }

            requiredRoles.forEach { requiredRole ->
                if (!hasRole(navAnsattRoles, requiredRole)) {
                    return@on call.respond(
                        HttpStatusCode.Companion.Forbidden,
                        "Mangler følgende rolle: ${requiredRole.rolle}",
                    )
                }
            }
        }
    }
}

private fun hasRole(
    roles: Set<NavAnsattRolle>,
    requiredRole: NavAnsattRolle,
): Boolean = when (requiredRole.generell) {
    true -> roles.any { it.rolle == requiredRole.rolle }
    false -> roles.any {
        it.rolle == requiredRole.rolle && (it.generell || it.enheter.containsAll(requiredRole.enheter))
    }
}

private val AuthorizedRolesKey = AttributeKey<List<NavAnsattRolle>>("AuthorizedRolesKey")

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

private class NavAnsattAuthorizationRouteSelector(val roles: List<Rolle>) : RouteSelector() {
    override suspend fun evaluate(context: RoutingResolveContext, segmentIndex: Int): RouteSelectorEvaluation {
        return RouteSelectorEvaluation.Companion.Transparent
    }

    override fun toString(): String = "(authorize ${roles.joinToString { it.name }})"
}
