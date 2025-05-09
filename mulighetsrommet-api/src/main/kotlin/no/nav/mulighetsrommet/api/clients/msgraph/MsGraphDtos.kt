package no.nav.mulighetsrommet.api.clients.msgraph

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.*

@Serializable
internal data class AddMemberRequest(
    @SerialName("@odata.id")
    val odataId: String,
)

@Serializable
internal data class GetGroupMembersResponse(
    val value: List<MsGraphUserDto>,
)

@Serializable
internal data class GetMemberGroupsRequest(
    val securityEnabledOnly: Boolean,
)

@Serializable
internal data class GetMemberGroupsResponse(
    val value: List<
        @Serializable(with = UUIDSerializer::class)
        UUID,
        >,
)

@Serializable
internal data class GetUserSearchResponse(
    val value: List<MsGraphUserDto>,
)
