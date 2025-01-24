package no.nav.mulighetsrommet.api.avtale

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.*

@Serializable
data class FrikobleKontaktpersonRequest(
    @Serializable(with = UUIDSerializer::class)
    val kontaktpersonId: UUID,
    @Serializable(with = UUIDSerializer::class)
    val dokumentId: UUID,
)
