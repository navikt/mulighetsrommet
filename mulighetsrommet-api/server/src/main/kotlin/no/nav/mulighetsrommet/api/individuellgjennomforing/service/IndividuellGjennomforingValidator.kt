package no.nav.mulighetsrommet.api.individuellgjennomforing.service

import no.nav.mulighetsrommet.api.individuellgjennomforing.api.IndividuellGjennomforingRequest
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.validation.Validated
import no.nav.mulighetsrommet.api.validation.validation

object IndividuellGjennomforingValidator {
    fun validate(request: IndividuellGjennomforingRequest): Validated<IndividuellGjennomforingRequest> = validation {
        validate(request.navn.isNotBlank()) {
            FieldError.of("Navn er påkrevd", IndividuellGjennomforingRequest::navn)
        }
        validate(request.navn.length <= 500) {
            FieldError.of("Navn kan ikke være lengre enn 500 tegn", IndividuellGjennomforingRequest::navn)
        }
        validate(request.administratorer.isNotEmpty()) {
            FieldError.of("Du må velge minst én administrator", IndividuellGjennomforingRequest::administratorer)
        }
        request
    }
}
