package no.nav.mulighetsrommet.api.amo

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.janzz.Sertifisering
import no.nav.mulighetsrommet.serializers.UUIDListSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

@Serializable
data class OpplaringKategoriseringRequest(
    @Serializable(with = UUIDSerializer::class)
    val kurstypeId: UUID? = null,
    @Serializable(with = UUIDSerializer::class)
    val bransjeId: UUID? = null,
    val sertifiseringer: Set<Sertifisering>? = null,
    @Serializable(with = UUIDListSerializer::class)
    val forerkort: List<UUID>? = null,
    val innholdElementer: Set<OpplaringKategorisering.InnholdElement>? = null,
    val norskprove: Boolean? = null,
    @Serializable(with = UUIDSerializer::class)
    val utdanningsprogramId: UUID? = null,
    @Serializable(with = UUIDListSerializer::class)
    val larefag: List<UUID>? = null,
)
