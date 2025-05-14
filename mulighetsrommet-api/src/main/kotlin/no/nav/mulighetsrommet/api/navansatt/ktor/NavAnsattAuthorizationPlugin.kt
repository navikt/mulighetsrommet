package no.nav.mulighetsrommet.api.navansatt.ktor

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.navansatt.service.NavAnsattPrincipal
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.ProblemDetail

class NavAnsattAuthorizationPluginConfiguration {
    var requiredRoles: List<Set<Rolle>> = listOf()
}

val NavAnsattAuthorizationPlugin = createRouteScopedPlugin(
    name = "NavAnsattAuthorizationPlugin",
    createConfiguration = ::NavAnsattAuthorizationPluginConfiguration,
) {
    val requiredRoles = pluginConfig.requiredRoles

    require(requiredRoles.isNotEmpty()) { "Minst én rolle er påkrevd" }

    pluginConfig.apply {
        on(AuthenticationChecked) { call ->
            val principal = call.principal<NavAnsattPrincipal>()

            if (principal == null) {
                return@on call.respond(HttpStatusCode.Unauthorized)
            }

            if (principal.roller.isEmpty()) {
                return@on call.respond(
                    HttpStatusCode.Forbidden,
                    NavAnsattManglerTilgang(principal.navIdent, requiredRoles.first().first()),
                )
            }

            val navAnsattRoles = principal.roller.map { it.rolle }

            requiredRoles.forEach { anyOfRoles ->
                if (anyOfRoles.intersect(navAnsattRoles).isEmpty()) {
                    return@on call.respond(
                        HttpStatusCode.Forbidden,
                        NavAnsattManglerTilgang(principal.navIdent, anyOfRoles.first()),
                    )
                }
            }
        }
    }
}

@Serializable
class NavAnsattManglerTilgang(
    val navIdent: NavIdent,
    val missingRole: Rolle,
    override val title: String = "Mangler tilgang til ressurs",
) : ProblemDetail() {
    override val type = "mangler-tilgang"
    override val status: Int = HttpStatusCode.Forbidden.value
    override val detail: String =
        "rollen '$missingRole' er påkrevd for å få tilgang til denne ressursen"
    override val extensions = mapOf(
        "missingRole" to Json.encodeToJsonElement(missingRole),
    )
    override val instance = navIdent.value
}
