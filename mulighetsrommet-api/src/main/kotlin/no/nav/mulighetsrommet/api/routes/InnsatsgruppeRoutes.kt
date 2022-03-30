package no.nav.mulighetsrommet.api.routes

import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import no.nav.mulighetsrommet.api.services.InnsatsgruppeService
import org.koin.ktor.ext.inject

fun Route.innsatsgruppeRoutes() {

    val innsatsgruppeService: InnsatsgruppeService by inject()

    get("/api/innsatsgrupper") {
        call.respond(innsatsgruppeService.getInnsatsgrupper())
    }
}
