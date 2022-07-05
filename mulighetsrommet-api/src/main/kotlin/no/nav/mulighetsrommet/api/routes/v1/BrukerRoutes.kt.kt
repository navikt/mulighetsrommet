package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.services.BrukerService
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

fun Route.brukerRoutes() {
    val log = LoggerFactory.getLogger(this.javaClass)
    val brukerService: BrukerService by inject()

    route("/api/v1/bruker") {
        get("{fnr}") {
            // TODO hent ut autorization token, oversett til nytt format og send med til backend for api'ene vi kaller hos OBO
            val fnr = call.parameters["fnr"] ?: return@get call.respondText(
                "Mangler eller ugyldig fnr",
                status = HttpStatusCode.BadRequest
            )
            log.info("Henter brukerdata for bruker med fnr: $fnr")
            call.respond(brukerService.hentBrukerdata(fnr))
        }
    }
}
