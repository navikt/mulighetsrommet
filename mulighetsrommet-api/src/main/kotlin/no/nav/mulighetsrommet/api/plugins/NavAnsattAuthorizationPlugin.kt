package no.nav.mulighetsrommet.api.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import no.nav.mulighetsrommet.api.navansatt.NavAnsattPrincipal
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsattRolle
import no.nav.mulighetsrommet.api.navansatt.model.Rolle

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
                return@on call.respond(HttpStatusCode.Unauthorized)
            }

            val navAnsattRoles = principal.roller

            if (navAnsattRoles.isEmpty()) {
                return@on call.respond(
                    HttpStatusCode.Forbidden,
                    "Mangler roller minst én av følgende roller: ${requiredRoles.map { it.rolle }.joinToString(", ")}",
                )
            }

            requiredRoles.forEach { requiredRole ->
                if (!hasRole(navAnsattRoles, requiredRole)) {
                    return@on call.respond(
                        HttpStatusCode.Forbidden,
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

    val combinedRoles = parentRoles + routeRoles.map { NavAnsattRolle.generell(it) }

    authorizedRoute.attributes.put(AuthorizedRolesKey, combinedRoles)

    authorizedRoute.install(NavAnsattAuthorizationPlugin) {
        roles = combinedRoles
    }

    authorizedRoute.build()
    return authorizedRoute
}

private class NavAnsattAuthorizationRouteSelector(val roles: List<Rolle>) : RouteSelector() {
    override suspend fun evaluate(context: RoutingResolveContext, segmentIndex: Int): RouteSelectorEvaluation {
        return RouteSelectorEvaluation.Transparent
    }

    override fun toString(): String = "(authorize ${roles.joinToString { it.name }})"
}
