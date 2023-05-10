package no.nav.mulighetsrommet.api.clients.enhetsregister

import no.nav.mulighetsrommet.api.domain.dto.VirksomhetDto

interface AmtEnhetsregisterClient {
    suspend fun hentVirksomhet(virksomhetsnummer: String): VirksomhetDto?
}
