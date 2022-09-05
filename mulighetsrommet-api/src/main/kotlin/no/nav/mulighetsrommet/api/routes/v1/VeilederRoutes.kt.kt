package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.services.VeilederService
import no.nav.mulighetsrommet.api.utils.getAccessToken
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

fun Route.veilederRoutes() {
    val veilederService: VeilederService by inject()
    val log = LoggerFactory.getLogger(this.javaClass)

    route("/api/v1/veileder") {
        get {
            val accessToken = call.getAccessToken()
            val callId = call.request.header(HttpHeaders.XRequestId)

            log.info("Innkommende request id $callId")
            call.respond(veilederService.hentVeilederdata(accessToken, callId))
        }
    }
}
