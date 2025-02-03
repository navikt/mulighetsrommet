package no.nav.mulighetsrommet.ktor.exception

import io.ktor.http.*
import no.nav.mulighetsrommet.model.ProblemDetail

/**
 * Thrown instances of [StatusException] originated from a ktor route handler will get caught by the
 * [no.nav.mulighetsrommet.ktor.plugins.configureStatusPages] ktor plugin, as long
 * as it's installed in the current application.
 */
open class StatusException(val status: HttpStatusCode, val detail: String) :
    Exception("Request failed with status: $status. Description: $detail")

fun StatusException.toProblemDetail(requestId: String): ProblemDetail {
    val description = status.description
    val detail = detail
    val statusInt = status.value
    return object : ProblemDetail() {
        override val type = "status-exception"
        override val title = description
        override val status = statusInt
        override val detail = detail
        override val instance = null
        override val extensions = mapOf("requestId" to requestId)
    }
}
