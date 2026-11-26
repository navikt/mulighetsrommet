package no.nav.mulighetsrommet.api.utbetaling.service

import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.validation.FieldError

sealed interface UtbetalingError {
    data class ManglerTilgang(val message: String, val roller: Set<Rolle>) : UtbetalingError
    data class Valideringsfeil(val errors: List<FieldError>) : UtbetalingError
    data class NotFound(val message: String) : UtbetalingError
    data object SafFeil : UtbetalingError
}
