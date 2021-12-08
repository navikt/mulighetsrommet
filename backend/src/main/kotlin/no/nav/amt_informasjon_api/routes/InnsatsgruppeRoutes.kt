package no.nav.amt_informasjon_api.routes

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.amt_informasjon_api.services.InnsatsgruppeService

fun Route.innsatsgruppeRoutes(service: InnsatsgruppeService) {
    route("/api/innsatsgrupper") {
        get {
            call.respond(service.getInnsatsgrupper())
        }
    }
}
