package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.api.services.NavEnhetService
import no.nav.mulighetsrommet.api.utils.getEnhetFilter
import org.koin.ktor.ext.inject

fun Route.navEnhetRoutes() {
    val navEnhetService: NavEnhetService by inject()

    route("api/v1/internal/nav-enheter") {
        get {
            val filter = getEnhetFilter()
            call.respond(navEnhetService.hentAlleEnheter(filter))
        }

        get("regioner") {
            call.respond(navEnhetService.hentRegioner())
        }

        get("{enhetsnummer}/overordnet") {
            val enhetsnummer: String by call.parameters
            val overordnetEnhet =
                navEnhetService.hentOverordnetFylkesenhet(enhetsnummer) ?: return@get call.respondText(
                    text = "Fant ikke overordnet enhet for enhetsnummer: $enhetsnummer",
                    status = HttpStatusCode.NotFound,
                )

            call.respond(overordnetEnhet)
        }
    }
}
