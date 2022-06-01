package no.nav.mulighetsrommet.api.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.services.InnsatsgruppeService
import org.koin.ktor.ext.inject

fun Route.innsatsgruppeRoutes() {

    val innsatsgruppeService: InnsatsgruppeService by inject()

    get("/api/innsatsgrupper") {
        call.respond(innsatsgruppeService.getInnsatsgrupper())
    }
}
