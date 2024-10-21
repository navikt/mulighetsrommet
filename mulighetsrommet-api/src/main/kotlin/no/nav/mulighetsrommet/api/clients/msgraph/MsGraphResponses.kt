package no.nav.mulighetsrommet.api.clients.msgraph

import kotlinx.serialization.Serializable

@Serializable
internal data class GetGroupMembersResponse(
    val value: List<MsGraphUserDto>,
)

@Serializable
internal data class GetMemberGroupsResponse(
    val value: List<MsGraphGroup>,
)

@Serializable
internal data class GetUserSearchResponse(
    val value: List<MsGraphUserDto>,
)
