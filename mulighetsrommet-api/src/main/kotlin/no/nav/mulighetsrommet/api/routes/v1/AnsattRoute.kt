package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.plugins.getNavAnsattAzureId
import no.nav.mulighetsrommet.api.services.AnsattService
import no.nav.mulighetsrommet.api.utils.getAccessToken
import org.koin.ktor.ext.inject

fun Route.ansattRoute() {
    val ansattService: AnsattService by inject()

    route("/api/v1/ansatt/me") {
        get {
            val accessToken = call.getAccessToken()
            call.respond(ansattService.hentAnsattData(accessToken, getNavAnsattAzureId()))
        }
    }
}
