package no.nav.mulighetsrommet.api.clients.msgraph

import no.nav.mulighetsrommet.api.domain.MSGraphBrukerdata
import java.util.UUID

interface MicrosoftGraphClient {
    suspend fun hentHovedenhetForBruker(navAnsattAzureId: UUID): MSGraphBrukerdata
}
