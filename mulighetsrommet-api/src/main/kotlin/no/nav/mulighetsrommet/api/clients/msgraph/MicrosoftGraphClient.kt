package no.nav.mulighetsrommet.api.clients.msgraph

import no.nav.mulighetsrommet.api.domain.MSGraphBrukerdata
import java.util.*

interface MicrosoftGraphClient {
    suspend fun hentHovedenhetForBruker(accesssToken: String, navAnsattAzureId: UUID): MSGraphBrukerdata
}
