package no.nav.mulighetsrommet.ktor.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.ktor.exception.BadRequest
import no.nav.mulighetsrommet.ktor.exception.InternalServerError
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.ktor.exception.toProblemDetail
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.securelog.SecureLog
import org.slf4j.MDC

fun Application.configureStatusPages() {

    fun logException(statusCode: HttpStatusCode, cause: Throwable, call: ApplicationCall) {
        val message = "${statusCode.description} (${statusCode.value}) on method: ${call.request.httpMethod.value} ${call.request.path()}: ${cause.message}"
        SecureLog.logger.error(message, cause)
        when {
            statusCode.value >= 500 -> log.error(message)
            statusCode.value >= 400 -> log.warn(message)
            else -> log.info(message)
        }
    }

    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            val requestId = MDC.get("correlationId")
            val problemDetail = BadRequest(
                detail = cause.message ?: "IllegalArgumentException",
                extensions = mapOf("requestId" to requestId),
            )
            logException(HttpStatusCode.BadRequest, cause, call)
            call.respondWithProblemDetail(problemDetail)
        }

        exception<StatusException> { call, cause ->
            val requestId = MDC.get("correlationId")
            logException(cause.status, cause, call)
            call.respondWithProblemDetail(cause.toProblemDetail(requestId))
        }

        exception<Throwable> { call, cause ->
            val requestId = MDC.get("correlationId")
            val problemDetail = InternalServerError(
                detail = "Internal Server Error",
                extensions = mapOf("requestId" to requestId),
            )

            logException(HttpStatusCode.InternalServerError, cause, call)
            call.respondWithProblemDetail(problemDetail)
        }
    }
}

suspend fun ApplicationCall.respondWithProblemDetail(problem: ProblemDetail) {
    respondText(
        text = Json.encodeToString(problem),
        status = HttpStatusCode.fromValue(problem.status),
        contentType = ContentType.Application.ProblemJson,
    )
}
