package no.nav.mulighetsrommet.ktor.plugins

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.CannotTransformContentToTypeException
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.PayloadTooLargeException
import io.ktor.server.plugins.UnsupportedMediaTypeException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.respondText
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.ktor.exception.BadRequest
import no.nav.mulighetsrommet.ktor.exception.InternalServerError
import no.nav.mulighetsrommet.ktor.exception.NotFound
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.ktor.exception.toProblemDetail
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
        status(HttpStatusCode.Unauthorized) { status ->
            val requestId = MDC.get("correlationId")

            call.respondWithProblemDetail(
                object : ProblemDetail() {
                    override val type = "unauthorized"
                    override val title = "Unauthorized"
                    override val status = status.value
                    override val detail =
                        "Du har ikke tilgang til denne tjenesten. Logg inn på nytt hvis du nylig har fått tilgang."
                    override val instance = null
                    override val extensions = mapOf("requestId" to requestId)
                },
            )
        }

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
