package no.nav.mulighetsrommet.api.setup.oauth

import kotlinx.serialization.Contextual

@kotlinx.serialization.Serializable
data class ThrowableErrorMessage(
    val message: String,
    @Contextual
    val throwable: Throwable
) {
    fun toErrorResponse() = ErrorResponse(message, throwable.toString())
}

@kotlinx.serialization.Serializable
data class ErrorResponse(
    val message: String,
    val cause: String
)
