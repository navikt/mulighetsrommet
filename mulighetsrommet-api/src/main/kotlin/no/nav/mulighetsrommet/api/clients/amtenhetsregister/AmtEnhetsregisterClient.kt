package no.nav.mulighetsrommet.api.clients.amtenhetsregister

import no.nav.mulighetsrommet.api.domain.VirksomhetDTO

interface AmtEnhetsregisterClient {
    suspend fun hentVirksomhetsNavn(virksomhetsnummer: Int): VirksomhetDTO?
}
