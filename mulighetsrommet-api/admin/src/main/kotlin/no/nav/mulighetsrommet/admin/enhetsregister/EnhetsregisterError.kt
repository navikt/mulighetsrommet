package no.nav.mulighetsrommet.admin.enhetsregister

sealed interface EnhetsregisterError {
    data class UgyldigSok(val message: String = "'sok' kan ikke være en tom streng") : EnhetsregisterError
    data object IkkeFunnet : EnhetsregisterError
    data object Feil : EnhetsregisterError
}
