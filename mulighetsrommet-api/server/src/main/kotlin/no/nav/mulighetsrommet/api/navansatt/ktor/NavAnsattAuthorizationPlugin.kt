package no.nav.mulighetsrommet.api.navansatt.ktor

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.auth.AuthenticationChecked
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.api.navansatt.service.NavAnsattMedRollerPrincipal
import no.nav.mulighetsrommet.ktor.exception.Unauthorized
import no.nav.mulighetsrommet.model.ProblemDetail

class NavAnsattAuthorizationPluginConfiguration {
    var requiredRoles: RequiredRoles = RequiredRoles(emptyList())
}

data class RequiredRoles(
    val roles: List<AnyRoles>,
)

data class AnyRoles(
    val roles: Set<Rolle>,
)

val NavAnsattAuthorizationPlugin = createRouteScopedPlugin(
    name = "NavAnsattAuthorizationPlugin",
    createConfiguration = ::NavAnsattAuthorizationPluginConfiguration,
) {
    val requiredRoles = pluginConfig.requiredRoles

    require(requiredRoles.roles.isNotEmpty()) { "Minst én rolle er påkrevd" }

    pluginConfig.apply {
        on(AuthenticationChecked) { call ->
            val principal =
                call.principal<NavAnsattMedRollerPrincipal>()
                    ?: return@on call.respond(
                        Unauthorized(
                            detail = "Du må være autentisert som Nav-ansatt for å få tilgang til denne ressursen.",
                        ),
                    )

            if (principal.roller.isEmpty()) {
                return@on call.respond(
                    NavAnsattManglerTilgang(requiredRoles.roles.first().roles),
                )
            }

            val navAnsattRoles = principal.roller.map { it.rolle }.toSet()
            requiredRoles.roles.forEach { anyOfRoles ->
                if (anyOfRoles.roles.intersect(navAnsattRoles).isEmpty()) {
                    return@on call.respond(
                        NavAnsattManglerTilgang(anyOfRoles.roles),
                    )
                }
            }
        }
    }
}

@Serializable
class NavAnsattManglerTilgang(
    val missingRoles: Set<Rolle>,
    override val title: String = "Mangler tilgang til ressurs",
) : ProblemDetail() {
    override val type = "mangler-tilgang"
    override val status: Int = HttpStatusCode.Forbidden.value
    override val detail: String =
        "Minst en av følgende roller er påkrevd for å få tilgang til denne ressursen:"
    override val extensions = mapOf(
        "missingRoles" to missingRoles.map { it.visningsnavn },
    )
    override val instance = null
}
