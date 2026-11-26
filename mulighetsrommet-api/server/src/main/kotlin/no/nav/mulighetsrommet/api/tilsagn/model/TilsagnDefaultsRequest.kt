@file:UseSerializers(UUIDSerializer::class)

package no.nav.mulighetsrommet.api.tilsagn.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

@Serializable
data class TilsagnDefaultsRequest(
    val gjennomforingId: UUID,
    val type: TilsagnType,
    val periodeStart: String? = null,
    val periodeSlutt: String? = null,
    val deltaker: UUID? = null,
)
