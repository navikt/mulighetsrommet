package no.nav.mulighetsrommet.api.navansatt.ktor

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsattRolle
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsattRolleHelper
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
                if (!NavAnsattRolleHelper.hasRole(navAnsattRoles, requiredRole)) {
                    return@on call.respond(
                        HttpStatusCode.Companion.Forbidden,
                        "Mangler følgende rolle: ${requiredRole.rolle}",
                    )
                }
            }
        }
    }
}
