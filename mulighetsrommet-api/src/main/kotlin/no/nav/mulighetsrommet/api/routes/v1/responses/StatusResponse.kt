package no.nav.mulighetsrommet.api.routes.v1.responses

import arrow.core.Either
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*

/**
 * Et forsøk på å definere noen utility-klasser som gjør det lettere å benytte Arrow's [Either] i kombinasjon
 * med ktor routes.
 */
typealias StatusResponse<T> = Either<StatusResponseError, T>

sealed class StatusResponseError(val status: HttpStatusCode, val message: String?) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StatusResponseError) return false

        if (status != other.status) return false
        if (message != other.message) return false

        return true
    }

    override fun hashCode(): Int {
        var result = status.hashCode()
        result = 31 * result + (message?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "StatusResponseError(status=$status, message=$message)"
    }
}

class ServerError(message: String? = null) : StatusResponseError(HttpStatusCode.InternalServerError, message)

class BadRequest(message: String? = null) : StatusResponseError(HttpStatusCode.BadRequest, message)

class NotFound(message: String? = null) : StatusResponseError(HttpStatusCode.NotFound, message)

class Forbidden(message: String? = null) : StatusResponseError(HttpStatusCode.Forbidden, message)

suspend inline fun <reified T : Any> ApplicationCall.respondWithStatusResponse(result: StatusResponse<T>) {
    result
        .onRight { message ->
            respond(message)
        }
        .onLeft { error ->
            respond(error.status, error.message ?: error.status.description)
        }
}
