package no.nav.mulighetsrommet.ktor.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.ktor.exception.BadRequest
import no.nav.mulighetsrommet.ktor.exception.InternalServerError
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.ktor.exception.toProblemDetail
import no.nav.mulighetsrommet.model.ProblemDetail
import org.slf4j.LoggerFactory
import org.slf4j.MDC

fun Application.configureStatusPages() {
    val logger = LoggerFactory.getLogger(javaClass)
    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            val requestId = MDC.get("correlationId")
            val problemDetail = BadRequest(
                detail = cause.message ?: "IllegalArgumentException",
                extensions = mapOf("requestId" to requestId),
            )
            call.respondWithProblemDetail(problemDetail)
        }

        exception<StatusException> { call, cause ->
            val requestId = MDC.get("correlationId")
            call.respondWithProblemDetail(cause.toProblemDetail(requestId))
        }

        exception<Throwable> { call, cause ->
            val requestId = MDC.get("correlationId")
            val problemDetail = InternalServerError(
                detail = "Internal Server Error",
                extensions = mapOf("requestId" to requestId),
            )
            logger.error("Internal Server Error - Cause: $cause")
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
