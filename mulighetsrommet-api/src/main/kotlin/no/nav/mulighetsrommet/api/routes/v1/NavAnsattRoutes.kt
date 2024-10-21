package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.api.plugins.getNavAnsattAzureId
import no.nav.mulighetsrommet.api.services.NavAnsattService
import no.nav.mulighetsrommet.api.utils.getNavAnsattFilter
import no.nav.mulighetsrommet.ktor.extensions.getAccessToken
import no.nav.mulighetsrommet.tokenprovider.AccessType
import org.koin.ktor.ext.inject

fun Route.navAnsattRoutes() {
    val ansattService: NavAnsattService by inject()

    route("/ansatt") {
        get {
            val filter = getNavAnsattFilter()
            val ansatte = ansattService.getNavAnsatte(filter = filter)
            call.respond(ansatte)
        }

        get("/sok") {
            val q: String by call.request.queryParameters

            val ansatte = ansattService.getNavAnsattFromAzureSok(query = q)
            call.respond(ansatte)
        }

        get("/me") {
            val azureId = getNavAnsattAzureId()
            val obo = AccessType.OBO(call.getAccessToken())
            val ansatt = ansattService.getOrSynchronizeNavAnsatt(azureId, obo)
            call.respond(ansatt)
        }
    }
}
