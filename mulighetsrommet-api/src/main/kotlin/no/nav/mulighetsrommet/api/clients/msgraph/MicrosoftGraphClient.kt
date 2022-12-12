package no.nav.mulighetsrommet.api.clients.msgraph

import java.util.UUID

interface MicrosoftGraphClient {
    suspend fun hentHovedenhetForBruker(navAnsattAzureId: UUID): String
}
