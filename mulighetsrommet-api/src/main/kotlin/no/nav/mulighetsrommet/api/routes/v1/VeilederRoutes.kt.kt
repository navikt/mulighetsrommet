package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.services.VeilederService
import no.nav.mulighetsrommet.api.utils.getAccessToken
import org.koin.ktor.ext.inject

fun Route.veilederRoutes() {
    val veilederService: VeilederService by inject()

    route("/api/v1/veileder") {
        get {
            val accessToken = call.getAccessToken()
            call.respond(veilederService.hentVeilederdata(accessToken))
        }
    }
}
