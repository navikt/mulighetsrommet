package no.nav.mulighetsrommet.api.tilsagn

import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.validation.FieldError

sealed interface TilsagnError {
    data class AgentUgyldigHandling(val message: String) : TilsagnError
    data class ManglerTilgang(val message: String, val navIdent: NavIdent, val roller: Set<Rolle>) : TilsagnError
    data class Valideringsfeil(val errors: List<FieldError>) : TilsagnError
}
