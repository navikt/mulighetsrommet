package no.nav.mulighetsrommet.ktor.plugins

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.CannotTransformContentToTypeException
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.PayloadTooLargeException
import io.ktor.server.plugins.UnsupportedMediaTypeException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText
import io.opentelemetry.api.trace.Span
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.ktor.exception.BadRequest
import no.nav.mulighetsrommet.ktor.exception.InternalServerError
import no.nav.mulighetsrommet.ktor.exception.NotFound
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.ktor.exception.Unathorized
import no.nav.mulighetsrommet.ktor.exception.toProblemDetail
import no.nav.mulighetsrommet.model.ProblemDetail

fun Application.configureStatusPages(logException: (HttpStatusCode, Throwable, ApplicationCall) -> Unit) {
    install(StatusPages) {
        status(HttpStatusCode.Unauthorized) {
            val traceId = Span.current().spanContext.traceId
            val problemDetail = Unathorized(
                detail = "Du har ikke tilgang til denne tjenesten. Logg inn på nytt hvis du nylig har fått tilgang.",
                extensions = mapOf("traceId" to traceId),
            )
            call.respondWithProblemDetail(problemDetail)
        }

        exception<IllegalArgumentException> { call, cause ->
            logException(HttpStatusCode.BadRequest, cause, call)

            val traceId = Span.current().spanContext.traceId
            val problemDetail = BadRequest(
                detail = cause.message ?: "IllegalArgumentException",
                extensions = mapOf("traceId" to traceId),
            )
            call.respondWithProblemDetail(problemDetail)
        }

        exception<StatusException> { call, cause ->
            logException(cause.status, cause, call)

            val traceId = Span.current().spanContext.traceId
            call.respondWithProblemDetail(cause.toProblemDetail(traceId))
        }

        exception<BadRequestException> { call, cause ->
            logException(HttpStatusCode.BadRequest, cause, call)

            val traceId = Span.current().spanContext.traceId
            val problemDetail = BadRequest(
                detail = cause.message ?: "BadRequestException",
                extensions = mapOf("traceId" to traceId),
            )
            call.respondWithProblemDetail(problemDetail)
        }

        exception<NotFoundException> { call, cause ->
            logException(HttpStatusCode.NotFound, cause, call)

            val traceId = Span.current().spanContext.traceId
            val problemDetail = NotFound(
                detail = cause.message ?: "NotFoundException",
                extensions = mapOf("traceId" to traceId),
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

            val traceId = Span.current().spanContext.traceId
            val problemDetail = statusCode.toProblemDetail(
                type = "content-transformation-error",
                detail = cause.message ?: "ContentTransformationException",
                extensions = mapOf("traceId" to traceId),
            )
            call.respondWithProblemDetail(problemDetail)
        }

        exception<Throwable> { call, cause ->
            logException(HttpStatusCode.InternalServerError, cause, call)

            val traceId = Span.current().spanContext.traceId
            val problemDetail = InternalServerError(
                detail = "Internal Server Error",
                extensions = mapOf("traceId" to traceId),
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
