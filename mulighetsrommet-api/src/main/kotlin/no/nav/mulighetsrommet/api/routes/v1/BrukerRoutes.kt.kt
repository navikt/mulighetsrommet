package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.services.BrukerService
import no.nav.mulighetsrommet.api.utils.getAccessToken
import org.koin.ktor.ext.inject

fun Route.brukerRoutes() {
    val brukerService: BrukerService by inject()

    val logger = application.environment.log

    route("/api/v1/bruker") {
        get {
            val fnr = call.request.queryParameters["fnr"] ?: return@get call.respondText(
                "Mangler eller ugyldig fnr",
                status = HttpStatusCode.BadRequest
            )
            val accessToken = call.getAccessToken()
            logger.debug("Request id fra inkommende request", call.request.header(HttpHeaders.XRequestId))
            call.respond(brukerService.hentBrukerdata(fnr, accessToken))
        }
    }
}
