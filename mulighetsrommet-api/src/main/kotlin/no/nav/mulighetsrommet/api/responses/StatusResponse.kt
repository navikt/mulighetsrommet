package no.nav.mulighetsrommet.api.responses

import arrow.core.Either
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.ktor.plugins.respondWithProblemDetail
import no.nav.mulighetsrommet.model.ProblemDetail
import kotlin.reflect.KProperty1

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

@Serializable
data class FieldError(
    val pointer: String,
    val detail: String,
) {
    companion object {
        fun ofPointer(pointer: String, detail: String): FieldError {
            return FieldError(pointer = pointer, detail = detail)
        }

        fun of(detail: String, vararg property: KProperty1<*, *>): FieldError {
            return FieldError(pointer = property.joinToString(prefix = "/", separator = "/") { it.name }, detail = detail)
        }

        fun root(detail: String): FieldError {
            return FieldError(pointer = "/", detail = detail)
        }
    }
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
