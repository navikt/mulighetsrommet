package no.nav.mulighetsrommet.api.datavarehus.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

@Serializable
data class DvhUtdanningslop(
    @Serializable(with = UUIDSerializer::class)
    val utdanningsprogram: UUID,
    val utdanninger: Set<
        @Serializable(with = UUIDSerializer::class)
        UUID,
        >,
)
