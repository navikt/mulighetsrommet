package no.nav.mulighetsrommet.api.domain.opplaring

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

@Serializable
data class Utdanningslop(
    @Serializable(with = UUIDSerializer::class)
    val utdanningsprogram: UUID,
    val utdanninger: Set<
        @Serializable(with = UUIDSerializer::class)
        UUID,
        >,
)
