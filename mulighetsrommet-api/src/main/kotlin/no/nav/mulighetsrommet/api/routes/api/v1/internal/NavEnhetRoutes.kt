package no.nav.mulighetsrommet.api.routes.api.v1.internal

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.services.NavEnhetService
import no.nav.mulighetsrommet.api.utils.getEnhetFilter
import org.koin.ktor.ext.inject

fun Route.navEnhetRoutes() {
    val navEnhetService: NavEnhetService by inject()

    route("api/v1/internal/enheter") {
        get {
            val filter = getEnhetFilter()
            call.respond(navEnhetService.hentAlleEnheter(filter))
        }
        get("/avtaler") {
            val filter = getEnhetFilter()
            call.respond(navEnhetService.hentEnheterForAvtale(filter))
        }
    }
}
