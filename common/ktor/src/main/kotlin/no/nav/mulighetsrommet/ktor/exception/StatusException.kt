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
    return when (status) {
        HttpStatusCode.InternalServerError -> InternalServerError(detail)
        HttpStatusCode.BadRequest -> BadRequest(detail)
        HttpStatusCode.Forbidden -> Forbidden(detail)
        HttpStatusCode.NotFound -> NotFound(detail)
        else -> {
            object : ProblemDetail() {
                override val type = "status-exception"
                override val title = description
                override val status = statusInt
                override val detail = detail
                override val instance = null
                override val extensions = mapOf("requestId" to requestId)
            }
        }
    }
}

fun HttpStatusCode.toProblemDetail(type: String, detail: String, extensions: Map<String, Any?>? = null) = object : ProblemDetail() {
    override val type = type
    override val title = description
    override val status = value
    override val detail = detail
    override val instance = null
    override val extensions = extensions
}

data class InternalServerError(
    override val detail: String,
    override val extensions: Map<String, Any?>? = null,
) : ProblemDetail() {
    override val type = "internal-server-error"
    override val title = HttpStatusCode.InternalServerError.description
    override val status: Int = HttpStatusCode.InternalServerError.value
    override val instance = null
}

data class BadRequest(
    override val detail: String,
    override val extensions: Map<String, Any?>? = null,
) : ProblemDetail() {
    override val type = "bad-request"
    override val title = HttpStatusCode.BadRequest.description
    override val status: Int = HttpStatusCode.BadRequest.value
    override val instance = null
}

data class NotFound(
    override val detail: String,
    override val extensions: Map<String, Any?>? = null,
) : ProblemDetail() {
    override val type = "not-found"
    override val title = HttpStatusCode.NotFound.description
    override val status: Int = HttpStatusCode.NotFound.value
    override val instance = null
}

data class Forbidden(
    override val detail: String,
    override val extensions: Map<String, Any?>? = null,
) : ProblemDetail() {
    override val type = "forbidden"
    override val title = HttpStatusCode.Forbidden.description
    override val status: Int = HttpStatusCode.Forbidden.value
    override val instance = null
}
