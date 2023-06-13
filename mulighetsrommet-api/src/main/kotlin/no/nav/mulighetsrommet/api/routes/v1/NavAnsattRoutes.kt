package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.plugins.getNavAnsattAzureId
import no.nav.mulighetsrommet.api.services.NavAnsattService
import no.nav.mulighetsrommet.api.utils.getAccessToken
import no.nav.mulighetsrommet.api.utils.getNavAnsattFilter
import org.koin.ktor.ext.inject

fun Route.navAnsattRoutes() {
    val ansattService: NavAnsattService by inject()

    route("/api/v1/internal/ansatt") {
        get {
            val filter = getNavAnsattFilter()
            call.respond(ansattService.hentAnsatte(filter))
        }
        get("/me") {
            val accessToken = call.getAccessToken()
            call.respond(ansattService.hentAnsattData(accessToken, getNavAnsattAzureId()))
        }
    }
}
