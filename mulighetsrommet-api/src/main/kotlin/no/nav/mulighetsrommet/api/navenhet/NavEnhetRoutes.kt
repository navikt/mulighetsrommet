package no.nav.mulighetsrommet.api.navenhet

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetStatus
import no.nav.mulighetsrommet.model.NavEnhetNummer
import org.koin.ktor.ext.inject

fun Route.navEnhetRoutes() {
    val navEnhetService: NavEnhetService by inject()

    route("nav-enheter") {
        get {
            val defaultFilter = EnhetFilter(
                statuser = listOf(
                    NavEnhetStatus.AKTIV,
                    NavEnhetStatus.UNDER_AVVIKLING,
                    NavEnhetStatus.UNDER_ETABLERING,
                ),
            )

            // TODO: ikke returner _alle_ enheter her, de fleste typene er egentlig ikke relevante i adminflate
            val enheter = navEnhetService.hentAlleEnheter(defaultFilter)

            call.respond(enheter)
        }

        get("regioner") {
            call.respond(navEnhetService.hentRegioner())
        }

        get("kostnadssted") {
            val regioner = call.parameters.getAll("regioner")?.map { NavEnhetNummer(it) } ?: emptyList()
            call.respond(navEnhetService.hentKostnadssted(regioner))
        }

        get("{enhetsnummer}/overordnet") {
            val enhetsnummer = call.parameters.getOrFail("enhetsnummer").let { NavEnhetNummer(it) }

            val overordnetEnhet = navEnhetService.hentOverordnetFylkesenhet(enhetsnummer)
                ?: return@get call.respondText(
                    text = "Fant ikke overordnet enhet for enhetsnummer: $enhetsnummer",
                    status = HttpStatusCode.NotFound,
                )

            call.respond(overordnetEnhet)
        }
    }
}

data class EnhetFilter(
    val statuser: List<NavEnhetStatus>? = null,
    val typer: List<NavEnhetType>? = null,
    val overordnetEnhet: NavEnhetNummer? = null,
)
