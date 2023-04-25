package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.util.*

@Serializable
data class ExchangeArenaIdForIdResponse(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
)
