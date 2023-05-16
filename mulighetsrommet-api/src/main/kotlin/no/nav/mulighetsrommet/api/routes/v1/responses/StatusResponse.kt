package no.nav.mulighetsrommet.api.routes.v1.responses

import arrow.core.Either
import io.ktor.http.*

/**
 * Et forsøk på å definere noen utility-klasser som gjør det lettere å benytte Arrow's [Either] i kombinasjon
 * med ktor routes.
 */
typealias StatusResponse<T> = Either<StatusResponseError, T>

sealed class StatusResponseError(val status: HttpStatusCode, val message: String)

class ServerError(message: String) : StatusResponseError(HttpStatusCode.InternalServerError, message)

class BadRequest(message: String = "Bad request") : StatusResponseError(HttpStatusCode.BadRequest, message)
