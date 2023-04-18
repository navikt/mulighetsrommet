package no.nav.mulighetsrommet.api.clients.msgraph

import java.util.*

interface MicrosoftGraphClient {
    suspend fun hentAnsattdata(accessToken: String, navAnsattAzureId: UUID): AnsattDataDTO
}
