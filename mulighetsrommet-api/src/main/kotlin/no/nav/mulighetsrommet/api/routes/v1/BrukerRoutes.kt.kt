package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.services.BrukerService
import no.nav.mulighetsrommet.api.utils.getAccessToken
import org.slf4j.LoggerFactory

fun Route.brukerRoutes(brukerService: BrukerService) {
    val log = LoggerFactory.getLogger(this.javaClass)

    route("/api/v1/bruker") {
        get("{fnr}") {
            val fnr = call.parameters["fnr"] ?: return@get call.respondText(
                "Mangler eller ugyldig fnr",
                status = HttpStatusCode.BadRequest
            )
            log.info("Henter brukerdata for bruker med fnr: $fnr")
            val accessToken = call.getAccessToken()
            log.info("Klarte å hente accessToken. Printer en substreng av access token [0-10]: {}", accessToken?.substring(0, 10))
            call.respond(brukerService.hentBrukerdata(fnr, accessToken))
        }
    }
}
