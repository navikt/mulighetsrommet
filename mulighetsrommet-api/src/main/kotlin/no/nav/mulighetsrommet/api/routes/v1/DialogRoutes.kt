package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.plugins.getNorskIdent
import no.nav.mulighetsrommet.api.services.DialogRequest
import no.nav.mulighetsrommet.api.services.DialogService
import no.nav.mulighetsrommet.api.services.PoaoTilgangService
import no.nav.mulighetsrommet.api.utils.getAccessToken
import org.koin.ktor.ext.inject

fun Route.dialogRoutes() {
    val dialogService: DialogService by inject()
    val poaoTilgangService: PoaoTilgangService by inject()

    route("/api/v1/dialog") {
        post {
            poaoTilgangService.verifyAccessToUserFromVeileder(getNavIdent(), getNorskIdent())
            val dialogRequest = call.receive<DialogRequest>()
            val fnr = call.request.queryParameters["fnr"] ?: return@post call.respondText(
                "Mangler eller ugyldig fnr",
                status = HttpStatusCode.BadRequest
            )
            val accessToken = call.getAccessToken()
            val response = dialogService.sendMeldingTilDialogen(fnr, accessToken, dialogRequest)
            response?.let { call.respond(response) } ?: call.respond(HttpStatusCode.Conflict)
        }
    }
}
