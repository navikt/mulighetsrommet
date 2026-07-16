package no.nav.mulighetsrommet.api.tiltakdokument.service

import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tiltakdokument.api.TiltakDokumentRequest
import no.nav.mulighetsrommet.api.validation.Validated
import no.nav.mulighetsrommet.api.validation.validation

object TiltakDokumentValidator {
    fun validate(request: TiltakDokumentRequest): Validated<TiltakDokumentRequest> = validation {
        validate(request.navn.isNotBlank()) {
            FieldError.of("Navn er påkrevd", TiltakDokumentRequest::navn)
        }
        validate(request.navn.length <= 500) {
            FieldError.of("Navn kan ikke være lengre enn 500 tegn", TiltakDokumentRequest::navn)
        }
        validate(request.administratorer.isNotEmpty()) {
            FieldError.of("Du må velge minst én administrator", TiltakDokumentRequest::administratorer)
        }
        request
    }
}
