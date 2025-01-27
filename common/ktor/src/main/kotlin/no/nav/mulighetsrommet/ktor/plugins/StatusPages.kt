package no.nav.mulighetsrommet.ktor.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.ktor.exception.StatusException
import org.slf4j.MDC

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            val requestId = MDC.get("correlationId")

            val status = HttpStatusCode.BadRequest

            val message = ErrorMessage(
                requestId = requestId,
                description = cause.message ?: status.description,
            )

            call.respond(status, message)
        }

        exception<StatusException> { call, cause ->
            val requestId = MDC.get("correlationId")

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
