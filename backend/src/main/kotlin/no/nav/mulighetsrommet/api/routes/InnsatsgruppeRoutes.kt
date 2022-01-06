package no.nav.mulighetsrommet.api.routes

import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import no.nav.mulighetsrommet.api.services.InnsatsgruppeService

fun Route.innsatsgruppeRoutes(service: InnsatsgruppeService) {
    get("/api/innsatsgrupper") {
        call.respond(service.getInnsatsgrupper())
    }
}
