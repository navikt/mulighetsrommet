package no.nav.mulighetsrommet.api.clients.brreg

import arrow.core.Either
import no.nav.mulighetsrommet.api.domain.dto.VirksomhetDto

interface BrregClient {
    suspend fun hentEnhet(orgnr: String): Either<Exception, VirksomhetDto>
    suspend fun sokEtterOverordnetEnheter(orgnr: String): List<VirksomhetDto>
}
