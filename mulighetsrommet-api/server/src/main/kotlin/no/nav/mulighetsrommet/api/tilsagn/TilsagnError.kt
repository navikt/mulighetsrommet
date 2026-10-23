package no.nav.mulighetsrommet.api.tilsagn

import no.nav.mulighetsrommet.validation.FieldError

sealed interface TilsagnError {
    data class ManglerTilgang(val message: String) : TilsagnError
    data class Valideringsfeil(val errors: List<FieldError>) : TilsagnError
}
