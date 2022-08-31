package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.services.DialogRequest
import no.nav.mulighetsrommet.api.services.DialogService
import org.koin.ktor.ext.inject

fun Route.dialogRoutes() {
    val dialogService: DialogService by inject()

    route("/api/v1/dialog") {
        post {
            val dialogRequest = call.receive<DialogRequest>()
            val fnr = call.request.queryParameters["fnr"] ?: return@post call.respondText(
                "Mangler eller ugyldig fnr",
                status = HttpStatusCode.BadRequest
            )
            val response = dialogService.sendMeldingTilDialogen(fnr, dialogRequest)
            response?.let { call.respond(response) } ?: call.respond(HttpStatusCode.Conflict)
        }
    }
}
