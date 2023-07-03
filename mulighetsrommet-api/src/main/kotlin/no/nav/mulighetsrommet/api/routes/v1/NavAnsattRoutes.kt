package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.plugins.getNavAnsattAzureId
import no.nav.mulighetsrommet.api.services.NavAnsattService
import no.nav.mulighetsrommet.api.services.NavVeilederService
import no.nav.mulighetsrommet.api.utils.getAccessToken
import no.nav.mulighetsrommet.api.utils.getNavAnsattFilter
import org.koin.ktor.ext.inject

fun Route.navAnsattRoutes() {
    val ansattService: NavAnsattService by inject()
    val veilederService: NavVeilederService by inject()

    route("/api/v1/internal") {
        get("/veileder/me") {
            val azureId = getNavAnsattAzureId()
            val oboToken = call.getAccessToken()
            call.respond(veilederService.getNavVeileder(azureId, oboToken))
        }

        route("/ansatt") {
            get {
                val filter = getNavAnsattFilter()
                val ansatte = ansattService.getNavAnsatte(filter = filter)
                call.respond(ansatte)
            }

            get("/me") {
                val azureId = getNavAnsattAzureId()
                val ansatt = ansattService.getOrSynchronizeNavAnsatt(azureId)
                call.respond(ansatt)
            }
        }
    }
}
