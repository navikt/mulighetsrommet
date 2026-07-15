package no.nav.mulighetsrommet.api.arrangor

import no.nav.mulighetsrommet.admin.arrangor.SyncArrangorError
import no.nav.mulighetsrommet.admin.enhetsregister.EnhetsregisterError
import no.nav.mulighetsrommet.ktor.exception.BadRequest
import no.nav.mulighetsrommet.ktor.exception.Gone
import no.nav.mulighetsrommet.ktor.exception.InternalServerError
import no.nav.mulighetsrommet.ktor.exception.NotFound
import no.nav.mulighetsrommet.model.ProblemDetail

fun SyncArrangorError.toProblemDetail(): ProblemDetail = when (this) {
    is SyncArrangorError.Enhetsregister -> error.toProblemDetail()

    is SyncArrangorError.FjernetAvJuridiskeArsaker ->
        Gone("Bediften $organisasjonsnummer er fjernet fra enhetsregisteret av juridiske årsaker den $slettetDato")
}

fun EnhetsregisterError.toProblemDetail(): ProblemDetail = when (this) {
    is EnhetsregisterError.UgyldigSok -> BadRequest(message)
    is EnhetsregisterError.IkkeFunnet -> NotFound("Fant ikke bedrift i enhetsregisteret")
    is EnhetsregisterError.Feil -> InternalServerError("Feil oppsto ved henting fra enhetsregisteret")
}
