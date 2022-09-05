package no.nav.mulighetsrommet.api.clients.veileder

import no.nav.mulighetsrommet.api.domain.VeilederDTO

interface VeilarbveilederClient {
    suspend fun hentVeilederdata(accessToken: String?, callId: String?): VeilederDTO?
}
