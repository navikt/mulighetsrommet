package no.nav.mulighetsrommet.arena_ords_proxy.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*


fun Application.configureErrorHandling() {
    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respondText(text = "400 (${HttpStatusCode.BadRequest.description}): $cause" , status = HttpStatusCode.BadRequest)
        }
        exception<RuntimeException> { call, cause ->
            call.respondText(text = "500 (${HttpStatusCode.InternalServerError.description}): $cause" , status = HttpStatusCode.InternalServerError)
        }
    }
}
