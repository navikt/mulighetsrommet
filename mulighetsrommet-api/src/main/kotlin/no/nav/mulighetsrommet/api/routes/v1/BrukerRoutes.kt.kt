package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.services.BrukerService
import no.nav.mulighetsrommet.api.utils.getAccessToken
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

fun Route.brukerRoutes() {
    val log = LoggerFactory.getLogger(this.javaClass)
    val brukerService: BrukerService by inject()

    route("/api/v1/bruker") {
        get {
            val fnr = call.request.queryParameters["fnr"] ?: return@get call.respondText(
                "Mangler eller ugyldig fnr",
                status = HttpStatusCode.BadRequest
            )
            val accessToken = call.getAccessToken()
            log.info("Request id fra inkommende request ${call.request.header(HttpHeaders.XRequestId)}")
            call.respond(brukerService.hentBrukerdata(fnr, accessToken))
        }
    }
}
