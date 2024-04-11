package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.util.*

@Serializable
data class FrikobleKontaktpersonRequest(
    @Serializable(with = UUIDSerializer::class)
    val kontaktpersonId: UUID,
    @Serializable(with = UUIDSerializer::class)
    val dokumentId: UUID,
)
