package no.nav.mulighetsrommet.api.arrangorflate.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnBeregningDto
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.*

@Serializable
data class ArrangorflateTilsagnDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val gjennomforing: ArrangorflateGjennomforingInfo,
    val bruktBelop: Int,
    val gjenstaendeBelop: Int,
    val tiltakstype: ArrangorflateTiltakstype,
    val type: TilsagnType,
    val periode: Periode,
    val beregning: TilsagnBeregningDto,
    val arrangor: ArrangorflateArrangor,
    val status: TilsagnStatus,
    val bestillingsnummer: String,
)

@Serializable
data class ArrangorflateTilsagnStatusOgAarsaker(
    val status: TilsagnStatus,
    val aarsaker: List<String>?,
)
