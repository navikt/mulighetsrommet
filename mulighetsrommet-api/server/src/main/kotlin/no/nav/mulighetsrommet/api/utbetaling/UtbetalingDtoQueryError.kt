package no.nav.mulighetsrommet.api.utbetaling

sealed class UtbetalingDtoQueryError {
    data class ManglerTilgang(val message: String) : UtbetalingDtoQueryError()
}
