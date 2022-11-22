package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.plugins.getNavIdent

@kotlinx.serialization.Serializable
data class Ansatt(
    val navIdent: String
)

fun Route.ansattRoute() {
    route("/api/v1/ansatt") {
        get {
            val navIdent = getNavIdent()
            call.respond(
                Ansatt(
                    navIdent = navIdent
                )
            )
        }
    }
}
