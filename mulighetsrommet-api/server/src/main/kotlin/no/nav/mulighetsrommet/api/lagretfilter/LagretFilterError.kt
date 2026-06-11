package no.nav.mulighetsrommet.api.lagretfilter

sealed class LagretFilterError {
    data class Forbidden(val message: String) : LagretFilterError()
}
