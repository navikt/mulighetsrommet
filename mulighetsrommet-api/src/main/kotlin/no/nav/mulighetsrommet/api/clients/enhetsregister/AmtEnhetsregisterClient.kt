package no.nav.mulighetsrommet.api.clients.enhetsregister

import no.nav.mulighetsrommet.api.domain.VirksomhetDTO

interface AmtEnhetsregisterClient {
    suspend fun hentVirksomhet(virksomhetsnummer: Int): VirksomhetDTO?
}
