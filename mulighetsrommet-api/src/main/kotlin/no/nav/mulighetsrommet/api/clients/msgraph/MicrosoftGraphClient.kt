package no.nav.mulighetsrommet.api.clients.msgraph

import no.nav.mulighetsrommet.api.domain.dto.AdGruppe
import java.util.*

interface MicrosoftGraphClient {
    suspend fun getNavAnsatt(navAnsattAzureId: UUID, oboToken: String? = null): AzureAdNavAnsatt

    suspend fun getMemberGroups(navAnsattAzureId: UUID, oboToken: String? = null): List<AdGruppe>

    suspend fun getGroupMembers(groupId: UUID): List<AzureAdNavAnsatt>
}
