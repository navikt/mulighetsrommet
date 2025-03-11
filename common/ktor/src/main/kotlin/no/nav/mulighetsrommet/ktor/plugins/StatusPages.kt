package no.nav.mulighetsrommet.ktor.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.ktor.exception.*
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.securelog.SecureLog
import org.slf4j.MDC

fun Application.configureStatusPages() {
    fun logException(statusCode: HttpStatusCode, cause: Throwable, call: ApplicationCall) {
        val statusDetails = "${statusCode.description} (${statusCode.value})"
        val requestDetails = "${call.request.httpMethod.value} ${call.request.path()}"
        val errorMessage = "$statusDetails on $requestDetails: ${cause.message}"

        SecureLog.logger.error(errorMessage, cause)

        val summary = "$errorMessage (se stacktrace i Securelogs)"
        when (statusCode.value) {
            in 500..599 -> log.error(summary)
            in 400..499 -> log.warn(summary)
            else -> log.info(summary)
        }
    }

    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            logException(HttpStatusCode.BadRequest, cause, call)

            val requestId = MDC.get("correlationId")
            val problemDetail = BadRequest(
                detail = cause.message ?: "IllegalArgumentException",
                extensions = mapOf("requestId" to requestId),
            )
            call.respondWithProblemDetail(problemDetail)
        }

        exception<StatusException> { call, cause ->
            logException(cause.status, cause, call)

            val requestId = MDC.get("correlationId")
            call.respondWithProblemDetail(cause.toProblemDetail(requestId))
        }

        exception<BadRequestException> { call, cause ->
            logException(HttpStatusCode.BadRequest, cause, call)

            val requestId = MDC.get("correlationId")
            val problemDetail = BadRequest(
                detail = cause.message ?: "BadRequestException",
                extensions = mapOf("requestId" to requestId),
            )
            call.respondWithProblemDetail(problemDetail)
        }

        exception<NotFoundException> { call, cause ->
            logException(HttpStatusCode.NotFound, cause, call)

            val requestId = MDC.get("correlationId")
            val problemDetail = NotFound(
                detail = cause.message ?: "NotFoundException",
                extensions = mapOf("requestId" to requestId),
            )
            call.respondWithProblemDetail(problemDetail)
        }

        exception<ContentTransformationException> { call, cause ->
            val statusCode = when (cause.cause) {
                is CannotTransformContentToTypeException -> HttpStatusCode.BadRequest
                is UnsupportedMediaTypeException -> HttpStatusCode.UnsupportedMediaType
                is PayloadTooLargeException -> HttpStatusCode.PayloadTooLarge
                else -> HttpStatusCode.InternalServerError
            }
            logException(statusCode, cause, call)

            val requestId = MDC.get("correlationId")
            val problemDetail = statusCode.toProblemDetail(
                type = "content-transformation-error",
                detail = cause.message ?: "ContentTransformationException",
                extensions = mapOf("requestId" to requestId),
            )
            call.respondWithProblemDetail(problemDetail)
        }

        exception<Throwable> { call, cause ->
            logException(HttpStatusCode.InternalServerError, cause, call)

            val requestId = MDC.get("correlationId")
            val problemDetail = InternalServerError(
                detail = "Internal Server Error",
                extensions = mapOf("requestId" to requestId),
            )
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
