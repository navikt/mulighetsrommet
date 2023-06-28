package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.plugins.getNavAnsattAzureId
import no.nav.mulighetsrommet.api.services.NavAnsattService
import no.nav.mulighetsrommet.api.services.NavVeilederService
import no.nav.mulighetsrommet.api.utils.getNavAnsattFilter
import org.koin.ktor.ext.inject

fun Route.navAnsattRoutes() {
    val ansattService: NavAnsattService by inject()
    val veilederService: NavVeilederService by inject()

    route("/api/v1/internal") {
        get("/veileder/me") {
            call.respond(veilederService.getNavVeileder(getNavAnsattAzureId()))
        }

        route("/ansatt") {
            get {
                val filter = getNavAnsattFilter()
                val ansatte = ansattService.getNavAnsatte(filter = filter)
                call.respond(ansatte)
            }

            get("/me") {
                call.respond(ansattService.getNavAnsatt(getNavAnsattAzureId()))
            }
        }
    }
}
