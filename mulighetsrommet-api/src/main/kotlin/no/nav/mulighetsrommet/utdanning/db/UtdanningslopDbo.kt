package no.nav.mulighetsrommet.utdanning.db

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.util.*

@Serializable
data class UtdanningslopDbo(
    @Serializable(with = UUIDSerializer::class)
    val utdanningsprogram: UUID,
    val utdanninger: List<
        @Serializable(with = UUIDSerializer::class)
        UUID,
        >,
)
