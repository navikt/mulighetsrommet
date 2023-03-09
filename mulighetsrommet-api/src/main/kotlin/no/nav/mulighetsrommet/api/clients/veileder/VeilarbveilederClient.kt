package no.nav.mulighetsrommet.api.clients.veileder

import java.util.*

interface VeilarbveilederClient {
    suspend fun hentVeilederdata(accessToken: String, navAnsattAzureId: UUID): VeilederDto?
}
