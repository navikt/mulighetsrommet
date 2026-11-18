package no.nav.mulighetsrommet.api.utbetaling

import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.model.NavIdent

sealed class UtbetalingDtoQueryError {
    data class ManglerTilgang(val message: String, val navIdent: NavIdent, val roller: Set<Rolle>) : UtbetalingDtoQueryError()
}
