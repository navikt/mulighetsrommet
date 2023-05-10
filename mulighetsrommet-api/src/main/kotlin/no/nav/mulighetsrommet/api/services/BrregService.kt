package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.clients.brreg.BrregClient
import no.nav.mulighetsrommet.api.clients.brreg.BrregEnhetDto
import no.nav.mulighetsrommet.api.clients.brreg.BrregEnhetUtenUnderenheterDto

class BrregService(private val brregClient: BrregClient) {

    suspend fun hentEnhet(orgnr: String): BrregEnhetDto {
        return brregClient.hentEnhet(orgnr)
    }

    suspend fun sokEtterEnhet(sokestreng: String): List<BrregEnhetUtenUnderenheterDto> {
        return brregClient.sokEtterOverordnetEnheter(sokestreng)
    }
}
