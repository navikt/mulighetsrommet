package no.nav.mulighetsrommet.api.clients.brreg

import no.nav.mulighetsrommet.api.domain.dto.VirksomhetDto

interface BrregClient {
    suspend fun hentEnhet(orgnr: String): VirksomhetDto?
    suspend fun sokEtterOverordnetEnheter(orgnr: String): List<VirksomhetDto>
}
