package no.nav.mulighetsrommet.ktor.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.ktor.exception.toProblemDetail
import no.nav.mulighetsrommet.model.ProblemDetail
import org.slf4j.MDC

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            val requestId = MDC.get("correlationId")
            val problemDetail = object : ProblemDetail() {
                override val type = "illegal-argument-exception"
                override val title = HttpStatusCode.BadRequest.description
                override val status = HttpStatusCode.BadRequest.value
                override val detail = cause.message ?: "IllegalArgumentException"
                override val instance = null
                override val extensions = mapOf("requestId" to requestId)
            }
            call.respondWithProblemDetail(problemDetail)
        }

        exception<StatusException> { call, cause ->
            val requestId = MDC.get("correlationId")
            call.respondWithProblemDetail(cause.toProblemDetail(requestId))
        }

        exception<Throwable> { call, _ ->
            val requestId = MDC.get("correlationId")
            val problemDetail = object : ProblemDetail() {
                override val type = "internal-server-error"
                override val title = HttpStatusCode.InternalServerError.description
                override val status = HttpStatusCode.InternalServerError.value
                override val detail = "Unknown Internal Server Error"
                override val instance = null
                override val extensions = mapOf("requestId" to requestId)
            }

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
