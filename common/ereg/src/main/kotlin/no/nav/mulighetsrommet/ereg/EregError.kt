package no.nav.mulighetsrommet.ereg

sealed interface EregError {
    data object BadRequest : EregError
    data object NotFound : EregError
    data class Error(val message: String) : EregError
}
