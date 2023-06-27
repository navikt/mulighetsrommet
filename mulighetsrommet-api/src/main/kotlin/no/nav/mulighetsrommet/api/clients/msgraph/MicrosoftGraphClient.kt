package no.nav.mulighetsrommet.api.clients.msgraph

import no.nav.mulighetsrommet.api.domain.dto.AdGruppe
import java.util.*

interface MicrosoftGraphClient {
    suspend fun getNavAnsatt(accessToken: String, navAnsattAzureId: UUID): AzureAdNavAnsatt

    suspend fun getMemberGroups(accessToken: String, navAnsattAzureId: UUID): List<AdGruppe>

    suspend fun getGroupMembers(groupId: UUID): List<AzureAdNavAnsatt>
}
