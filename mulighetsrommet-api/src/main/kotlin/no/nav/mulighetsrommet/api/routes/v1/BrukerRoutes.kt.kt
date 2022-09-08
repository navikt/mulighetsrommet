package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.services.BrukerService
import no.nav.mulighetsrommet.api.services.HistorikkService
import no.nav.mulighetsrommet.api.utils.getAccessToken
import org.koin.ktor.ext.inject

fun Route.brukerRoutes() {
    val brukerService: BrukerService by inject()
    val historikkService: HistorikkService by inject()

    route("/api/v1/bruker") {
        get {
            val fnr = call.request.queryParameters["fnr"] ?: return@get call.respondText(
                "Mangler eller ugyldig fnr",
                status = HttpStatusCode.BadRequest
            )
            val accessToken = call.getAccessToken()
            call.respond(brukerService.hentBrukerdata(fnr, accessToken))
        }
    }

    route("/api/v1/bruker/historikk") {
        get {
            val fnr = call.request.queryParameters["fnr"] ?: return@get call.respondText(
                "Mangler eller ugyldig fnr",
                status = HttpStatusCode.BadRequest
            )
            val accessToken = call.getAccessToken()
            call.respond(historikkService.hentHistorikkForBruker(fnr, accessToken))
        }
    }
}
