package no.nav.mulighetsrommet.api.clients.enhetsregister

interface AmtEnhetsregisterClient {
    suspend fun hentVirksomhet(virksomhetsnummer: String): VirksomhetDto?
}
