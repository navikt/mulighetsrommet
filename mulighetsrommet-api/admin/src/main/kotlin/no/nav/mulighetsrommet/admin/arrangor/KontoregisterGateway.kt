package no.nav.mulighetsrommet.admin.arrangor

import arrow.core.Either
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.Organisasjonsnummer

sealed interface KontoregisterError {
    data object IkkeFunnet : KontoregisterError
    data object Feil : KontoregisterError
}

interface KontoregisterGateway {
    suspend fun hentKontonummer(orgnr: Organisasjonsnummer): Either<KontoregisterError, Kontonummer>
}
