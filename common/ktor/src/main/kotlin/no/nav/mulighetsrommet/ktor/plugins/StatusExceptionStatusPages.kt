package no.nav.mulighetsrommet.ktor.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import no.nav.mulighetsrommet.ktor.exception.StatusException

fun Application.configureStatusPagesForStatusException() {
    install(StatusPages) {
        exception<StatusException> { call, cause ->
            call.respond(cause.status, cause.description ?: cause.status.description)
        }
    }
}
