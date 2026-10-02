package no.nav.mulighetsrommet.api.gjennomforing.service

import no.nav.mulighetsrommet.model.FieldError

sealed interface EnkeltplassBeslutningError {
    data class ManglerTilgang(val message: String) : EnkeltplassBeslutningError
    data class Valideringsfeil(val errors: List<FieldError>) : EnkeltplassBeslutningError
}
