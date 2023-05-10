package no.nav.mulighetsrommet.api.clients.brreg

interface BrregClient {
    suspend fun hentEnhet(orgnr: String): BrregEnhetDto
    suspend fun sokEtterOverordnetEnheter(orgnr: String): List<BrregEnhetUtenUnderenheterDto>
}
