package no.nav.mulighetsrommet.api.arrangorflate.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateArrangorDto
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.model.DataDetails
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

@Serializable
data class ArrangorflateTilsagnDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val tiltakstype: ArrangorflateTiltakstypeDto,
    val gjennomforing: ArrangorflateGjennomforingDto,
    val arrangor: ArrangorflateArrangorDto,
    val type: TilsagnType,
    val periode: Periode,
    val status: TilsagnStatus,
    val bruktBelop: ValutaBelop,
    val gjenstaendeBelop: ValutaBelop,
    val beregning: DataDetails,
    val bestillingsnummer: String,
    val beskrivelse: String?,
)

@Serializable
data class ArrangorflateTilsagnSummary(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val bestillingsnummer: String,
)
