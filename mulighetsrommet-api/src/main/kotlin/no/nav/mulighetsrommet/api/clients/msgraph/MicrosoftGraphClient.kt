package no.nav.mulighetsrommet.api.clients.msgraph

import java.util.*

interface MicrosoftGraphClient {
    suspend fun hentHovedenhetForBruker(accessToken: String, navAnsattAzureId: UUID): MSGraphBrukerHovedenhetDto
}
