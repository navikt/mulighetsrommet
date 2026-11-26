package no.nav.mulighetsrommet.api.utbetaling

import no.nav.mulighetsrommet.api.domain.navansatt.Rolle

sealed class UtbetalingDtoQueryError {
    data class ManglerTilgang(val message: String, val roller: Set<Rolle>) : UtbetalingDtoQueryError()
}
