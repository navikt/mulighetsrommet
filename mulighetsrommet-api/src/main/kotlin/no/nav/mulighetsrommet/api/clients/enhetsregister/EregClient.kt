package no.nav.mulighetsrommet.api.clients.enhetsregister

interface EregClient {
    suspend fun hentVirksomhet(virksomhetsnummer: String): EregVirksomhetDto?
}
