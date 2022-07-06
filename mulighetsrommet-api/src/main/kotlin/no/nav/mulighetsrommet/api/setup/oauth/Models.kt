package no.nav.mulighetsrommet.api.setup.oauth

data class ThrowableErrorMessage(
    val message: String,
    val throwable: Throwable
) {
    fun toErrorResponse() = ErrorResponse(message, throwable.toString())
}

data class ErrorResponse(
    val message: String,
    val cause: String
)
