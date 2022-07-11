package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.services.InnsatsgruppeService
import org.koin.ktor.ext.inject

fun Route.innsatsgruppeRoutes() {
    val innsatsgruppeService: InnsatsgruppeService by inject()

    route("/api/v1/innsatsgrupper") {
        get() {
            call.respond(innsatsgruppeService.getInnsatsgrupper())
        }
    }
}
