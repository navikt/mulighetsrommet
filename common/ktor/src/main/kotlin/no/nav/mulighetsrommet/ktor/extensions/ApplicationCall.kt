package no.nav.mulighetsrommet.ktor.extensions

import io.ktor.http.*
import io.ktor.http.auth.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import no.nav.mulighetsrommet.ktor.exception.StatusException

/**
 * Utility extension on [ApplicationCall] to get the bearer token from the request.
 *
 * A [StatusException] will be thrown if the bearer token is missing.
 */
fun ApplicationCall.getAccessToken(): String {
    val header = request.parseAuthorizationHeader()

    if (header is HttpAuthHeader.Single && header.authScheme.lowercase() in listOf("bearer")) {
        return header.blob
    }

    throw StatusException(HttpStatusCode.Unauthorized, "Mangler accesstoken - Ingen tilgang")
}
