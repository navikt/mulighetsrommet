package no.nav.mulighetsrommet.api.avtale.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

@Serializable
data class PrismodellDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val prismodellType: PrismodellType,
    val prisbetingelser: String?,
)
