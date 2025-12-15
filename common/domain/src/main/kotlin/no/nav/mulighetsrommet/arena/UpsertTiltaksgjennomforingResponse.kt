package no.nav.mulighetsrommet.arena

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

@Serializable
data class UpsertTiltaksgjennomforingResponse(
    @Serializable(with = UUIDSerializer::class)
    val sanityId: UUID?,
)
