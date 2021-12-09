package no.nav.amt_informasjon_api.routes

import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import no.nav.amt_informasjon_api.services.InnsatsgruppeService

fun Route.innsatsgruppeRoutes(service: InnsatsgruppeService) {
    get("/api/insatsgrupper") {
        call.respond(service.getInnsatsgrupper())
    }
}
