package no.nav.mulighetsrommet.ktor.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.ktor.exception.StatusException
import org.slf4j.MDC

fun Application.configureStatusPagesForStatusException() {
    install(StatusPages) {
        exception<StatusException> { call, cause ->
            val requestId = MDC.get("call-id")

            val message = ErrorMessage(
                requestId = requestId,
                description = cause.description ?: cause.status.description,
            )

            call.respond(cause.status, message)
        }
    }
}

@Serializable
data class ErrorMessage(
    val requestId: String? = null,
    val description: String,
)
