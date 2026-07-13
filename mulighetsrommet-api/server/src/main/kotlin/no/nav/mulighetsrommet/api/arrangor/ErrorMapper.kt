package no.nav.mulighetsrommet.api.arrangor

import no.nav.mulighetsrommet.admin.enhetsregister.EnhetsregisterError
import no.nav.mulighetsrommet.brreg.BrregError
import no.nav.mulighetsrommet.ktor.exception.BadRequest
import no.nav.mulighetsrommet.ktor.exception.Gone
import no.nav.mulighetsrommet.ktor.exception.InternalServerError
import no.nav.mulighetsrommet.ktor.exception.NotFound
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.ProblemDetail

fun ArrangorError.toProblemDetail(orgnr: Organisasjonsnummer? = null): ProblemDetail = when (this) {
    is ArrangorError.BrregError -> {
        if (orgnr != null) {
            this.error.toProblemDetail(orgnr)
        } else {
            this.error.toProblemDetail()
        }
    }
}

fun EnhetsregisterError.toProblemDetail(): ProblemDetail = when (this) {
    is EnhetsregisterError.UgyldigSok -> BadRequest(message)
    is EnhetsregisterError.Feil -> InternalServerError("Feil oppsto ved henting fra enhetsregisteret")
}

fun BrregError.toProblemDetail(orgnr: Organisasjonsnummer): ProblemDetail = when (this) {
    is BrregError.NotFound -> BadRequest("Fant ikke bedrift $orgnr i Brreg")
    is BrregError.FjernetAvJuridiskeArsaker -> Gone("Bediften $orgnr er fjernet fra Brreg av juridiske årsaker den ${this.enhet.slettetDato}")
    is BrregError.BadRequest, is BrregError.Error -> InternalServerError("Feil oppsto ved henting av bedrift $orgnr fra Brreg")
}

fun BrregError.toProblemDetail() = when (this) {
    is BrregError.NotFound -> NotFound("Not Found fra Brreg")
    is BrregError.FjernetAvJuridiskeArsaker -> Gone("Fjernet av juridiske årsaker fra Brreg ${this.enhet.slettetDato}")
    is BrregError.BadRequest, is BrregError.Error -> InternalServerError("Feil oppsto ved henting fra Brreg")
}
