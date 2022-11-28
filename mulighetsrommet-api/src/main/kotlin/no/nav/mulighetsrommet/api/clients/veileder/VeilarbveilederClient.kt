package no.nav.mulighetsrommet.api.clients.veileder

import no.nav.mulighetsrommet.api.domain.VeilederDTO
import java.util.*

interface VeilarbveilederClient {
    suspend fun hentVeilederdata(accessToken: String, navAnsattAzureId: UUID): VeilederDTO?
}
