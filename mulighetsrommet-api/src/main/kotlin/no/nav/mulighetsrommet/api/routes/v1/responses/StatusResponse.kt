package no.nav.mulighetsrommet.api.routes.v1.responses

import arrow.core.Either
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable
import kotlin.reflect.KProperty1

/**
 * Et forsøk på å definere noen utility-klasser som gjør det lettere å benytte Arrow's [Either] i kombinasjon
 * med ktor routes.
 */
typealias StatusResponse<T> = Either<StatusResponseError, T>

sealed class StatusResponseError(
    val status: HttpStatusCode,
    val message: String?,
    val errors: List<ValidationError>?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StatusResponseError) return false

        if (status != other.status) return false
        if (message != other.message) return false
        if (errors != other.errors) return false

        return true
    }

    override fun hashCode(): Int {
        var result = status.hashCode()
        result = 31 * result + (message?.hashCode() ?: 0)
        result = 31 * result + errors.hashCode()
        return result
    }

    override fun toString(): String {
        return "StatusResponseError(status=$status, message=$message, errors=$errors)"
    }
}

class ServerError(message: String? = null) : StatusResponseError(HttpStatusCode.InternalServerError, message, null)

class BadRequest(message: String? = null, errors: List<ValidationError>? = null) : StatusResponseError(
    HttpStatusCode.BadRequest,
    message,
    errors,
)

class NotFound(message: String? = null) : StatusResponseError(HttpStatusCode.NotFound, message, null)

class Forbidden(message: String? = null) : StatusResponseError(HttpStatusCode.Forbidden, message, null)

@Serializable
data class ValidationErrorResponse(
    val message: String?,
    val errors: List<ValidationError>,
)

@Serializable
data class ValidationError(
    val name: String,
    val message: String,
) {
    companion object {
        fun of(property: KProperty1<*, *>, message: String): ValidationError {
            return ValidationError(name = property.name, message = message)
        }

        fun ofCustomLocation(location: String, message: String): ValidationError {
            return ValidationError(name = location, message = message)
        }
    }
}

suspend inline fun <reified T : Any> ApplicationCall.respondWithStatusResponse(result: StatusResponse<T>) {
    result
        .onRight { message ->
            respond(message)
        }
        .onLeft { error ->
            respondWithStatusResponseError(error)
        }
}

suspend fun ApplicationCall.respondWithStatusResponseError(error: StatusResponseError) {
    if (error.errors == null) {
        val message = error.message ?: error.status.description
        respond(error.status, message)
    } else {
        val message = ValidationErrorResponse(message = error.message, errors = error.errors)
        respond(error.status, message)
    }
}
