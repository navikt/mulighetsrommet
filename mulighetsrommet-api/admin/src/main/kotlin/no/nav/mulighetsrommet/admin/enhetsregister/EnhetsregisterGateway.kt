package no.nav.mulighetsrommet.admin.enhetsregister

import arrow.core.Either
import no.nav.mulighetsrommet.model.Organisasjonsnummer

interface EnhetsregisterGateway {
    suspend fun sokHovedenheter(sok: String): Either<EnhetsregisterError, List<Hovedenhet>>

    suspend fun sokUnderenheter(sok: String): Either<EnhetsregisterError, List<Underenhet>>

    suspend fun hentVirksomhet(orgnr: Organisasjonsnummer): Either<EnhetsregisterError, VirksomhetOppslag>

    suspend fun hentUnderenheterForHovedenhet(orgnr: Organisasjonsnummer): Either<EnhetsregisterError, List<Underenhet>>
}
