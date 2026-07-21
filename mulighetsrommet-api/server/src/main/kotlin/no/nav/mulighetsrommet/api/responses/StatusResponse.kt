package no.nav.mulighetsrommet.api.responses

import arrow.core.Either
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.ktor.plugins.respondWithProblemDetail
import no.nav.mulighetsrommet.model.FieldError
import no.nav.mulighetsrommet.model.ProblemDetail

/**
 * Et forsøk på å definere noen utility-klasser som gjør det lettere å benytte Arrow's [Either] i kombinasjon
 * med ktor routes.
 */
typealias StatusResponse<T> = Either<ProblemDetail, T>

@Serializable
data class ValidationError(
    override val detail: String = "Unknown Validation Error",
    val errors: List<FieldError>,
) : ProblemDetail() {
    override val type = "validation-error"
    override val title = "Validation error"
    override val status: Int = HttpStatusCode.BadRequest.value
    override val extensions = mapOf("errors" to Json.encodeToJsonElement(errors))
    override val instance: String? = null
}

suspend inline fun <reified T : Any> ApplicationCall.respondWithStatusResponse(result: StatusResponse<T>) {
    result
        .onRight { message ->
            respond(message)
        }
        .onLeft { error ->
            respondWithProblemDetail(error)
        }
}
