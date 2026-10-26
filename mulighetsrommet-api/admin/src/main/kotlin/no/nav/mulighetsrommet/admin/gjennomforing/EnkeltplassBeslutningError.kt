package no.nav.mulighetsrommet.admin.gjennomforing

import no.nav.mulighetsrommet.validation.FieldError

sealed interface EnkeltplassBeslutningError {
    data class ManglerTilgang(val message: String) : EnkeltplassBeslutningError
    data class Valideringsfeil(val errors: List<FieldError>) : EnkeltplassBeslutningError
    data class ForslagTrukketTilbake(val detail: String) : EnkeltplassBeslutningError
}
