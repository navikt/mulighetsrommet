package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.clients.enhetsregister.AmtEnhetsregisterClient
import org.koin.ktor.ext.inject

fun Route.virksomhetRoutes() {
    val amtEnhetsregisterClientImpl: AmtEnhetsregisterClient by inject()
    route("api/v1/internal/virksomhet") {
        get("{orgnr}") {
            val orgnr = call.parameters["orgnr"] ?: return@get call.respondText(
                text = "Mangler eller ugyldig organisasjonsnummer",
                status = HttpStatusCode.BadRequest,
            )

            val enhet = amtEnhetsregisterClientImpl.hentVirksomhet(orgnr) ?: return@get call.respondText(
                text = "Fant ingen enhet med orgnr: $orgnr",
                status = HttpStatusCode.NotFound,
            )

            call.respond(enhet)
        }
    }
}
