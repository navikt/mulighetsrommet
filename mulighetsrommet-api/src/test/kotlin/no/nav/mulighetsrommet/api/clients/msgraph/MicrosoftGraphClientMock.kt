package no.nav.mulighetsrommet.api.clients.msgraph

import no.nav.mulighetsrommet.ktor.MockEngineBuilder
import no.nav.mulighetsrommet.ktor.respondJson
import java.util.*

fun MockEngineBuilder.mockMsGraphGetMemberGroups(oid: UUID, getGroups: () -> List<AdGruppe>) {
    get(".*/v1.0/users/$oid/transitiveMemberOf/microsoft.graph.group.*".toRegex()) {
        val groups = getGroups().map { MsGraphGroup(id = it.id, displayName = it.navn) }
        respondJson(GetMemberGroupsResponse(groups))
    }
}
