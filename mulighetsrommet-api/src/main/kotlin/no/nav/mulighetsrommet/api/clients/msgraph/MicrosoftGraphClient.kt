package no.nav.mulighetsrommet.api.clients.msgraph

import no.nav.mulighetsrommet.api.domain.dto.NavAnsattDto
import java.util.*

interface MicrosoftGraphClient {
    suspend fun getNavAnsatt(accessToken: String, navAnsattAzureId: UUID): NavAnsattDto

    suspend fun getGroupMembers(groupId: UUID): List<NavAnsattDto>
}
