package no.nav.mulighetsrommet.api.navenhet

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import io.ktor.util.pipeline.*
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetStatus
import org.koin.ktor.ext.inject

fun Route.navEnhetRoutes() {
    val navEnhetService: NavEnhetService by inject()

    route("nav-enheter") {
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

fun <T : Any> PipelineContext<T, ApplicationCall>.getEnhetFilter(): EnhetFilter {
    val statuser = call.parameters.getAll("statuser")
        ?.map { NavEnhetStatus.valueOf(it) }

    val typer = call.parameters.getAll("typer")
        ?.map { Norg2Type.valueOf(it) }

    return EnhetFilter(statuser = statuser, typer = typer)
}
