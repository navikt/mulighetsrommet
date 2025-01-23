package no.nav.mulighetsrommet.ktor.exception

import io.ktor.http.*

/**
 * Thrown instances of [StatusException] originated from a ktor route handler will get caught by the
 * [no.nav.mulighetsrommet.ktor.plugins.configureStatusPages] ktor plugin, as long
 * as it's installed in the current application.
 */
open class StatusException(val status: HttpStatusCode, val description: String? = null) :
    Exception("Request failed with status: $status. Description: $description")
