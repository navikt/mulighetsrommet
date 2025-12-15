package no.nav.mulighetsrommet.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

@Serializable
data class ExchangeArenaIdForIdResponse(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
)
