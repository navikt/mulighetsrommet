package no.nav.mulighetsrommet.ktor.extensions

import io.ktor.http.*
import io.ktor.server.application.*
import no.nav.mulighetsrommet.ktor.exception.StatusException

/**
 * Utility extension on [ApplicationCall] to get a path parameter by [name].
 *
 * A [StatusException] will be thrown if the parameter is null or empty.
 */
fun ApplicationCall.getNonEmptyPathParameter(name: String): String {
    val parameter = parameters[name]

    if (parameter.isNullOrEmpty()) {
        throw StatusException(HttpStatusCode.BadRequest, "Path parameter $name is missing")
    }

    return parameter
}

/**
 * Utility extension on [ApplicationCall] to get a query parameter by [name].
 *
 * A [StatusException] will be thrown if the parameter is null or empty.
 */
fun ApplicationCall.getNonEmptyQueryParameter(name: String): String {
    val parameter = request.queryParameters[name]

    if (parameter.isNullOrEmpty()) {
        throw StatusException(HttpStatusCode.BadRequest, "Query parameter $name is missing")
    }

    return parameter
}
