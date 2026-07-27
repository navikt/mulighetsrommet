package no.nav.mulighetsrommet.api.domain.opplaring

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

@Serializable
data class OpplaringKategorisering(
    @Serializable(with = UUIDSerializer::class)
    val kurstype: UUID? = null,
    @Serializable(with = UUIDSerializer::class)
    val bransje: UUID? = null,
    val forerkort: Set<
        @Serializable(with = UUIDSerializer::class)
        UUID,
        > = emptySet(),
    val innholdElementer: Set<
        @Serializable(with = UUIDSerializer::class)
        UUID,
        > = emptySet(),
    val norskprove: Boolean? = null,
    val sertifiseringer: Set<Sertifisering> = emptySet(),
    val utdanningslop: Utdanningslop? = null,
)
