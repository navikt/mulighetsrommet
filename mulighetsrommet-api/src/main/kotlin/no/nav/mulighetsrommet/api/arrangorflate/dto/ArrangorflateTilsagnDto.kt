package no.nav.mulighetsrommet.api.arrangorflate.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFastSatsPerTiltaksplassPerManed
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFri
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningPrisPerHeleUkesverk
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningPrisPerManedsverk
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningPrisPerUkesverk
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.model.DataDetails
import no.nav.mulighetsrommet.model.LabeledDataElement
import no.nav.mulighetsrommet.model.NorskIdent
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
    val deltakere: List<DeltakerPersonalia>,
) {
    @Serializable
    data class DeltakerPersonalia(
        @Serializable(with = UUIDSerializer::class)
        val deltakerId: UUID,
        val norskIdent: NorskIdent?,
        val navn: String,
    )

    companion object {
        fun from(tilsagn: Tilsagn, deltakere: List<DeltakerPersonalia>) = ArrangorflateTilsagnDto(
            id = tilsagn.id,
            gjennomforing = ArrangorflateGjennomforingDto(
                id = tilsagn.gjennomforing.id,
                lopenummer = tilsagn.gjennomforing.lopenummer,
                navn = tilsagn.gjennomforing.navn,
            ),
            bruktBelop = tilsagn.belopBrukt,
            gjenstaendeBelop = tilsagn.gjenstaendeBelop(),
            tiltakstype = ArrangorflateTiltakstypeDto(
                navn = tilsagn.tiltakstype.navn,
                tiltakskode = tilsagn.tiltakstype.tiltakskode,
            ),
            type = tilsagn.type,
            periode = tilsagn.periode,
            beregning = toArrangorflateTilsagnBeregningDetails(tilsagn),
            arrangor = ArrangorflateArrangorDto(
                id = tilsagn.arrangor.id,
                navn = tilsagn.arrangor.navn,
                organisasjonsnummer = tilsagn.arrangor.organisasjonsnummer,
            ),
            status = tilsagn.status,
            bestillingsnummer = tilsagn.bestilling.bestillingsnummer,
            beskrivelse = tilsagn.beskrivelse,
            deltakere = deltakere,
        )
    }
}

private fun toArrangorflateTilsagnBeregningDetails(tilsagn: Tilsagn): DataDetails {
    val entries = when (tilsagn.beregning) {
        is TilsagnBeregningFri -> listOf(
            LabeledDataElement.periode("Tilsagnsperiode", tilsagn.periode),
            LabeledDataElement.money("Totalbeløp", tilsagn.beregning.output.pris),
            LabeledDataElement.money("Gjenstående beløp", tilsagn.gjenstaendeBelop()),
        )

        is TilsagnBeregningFastSatsPerTiltaksplassPerManed -> listOf(
            LabeledDataElement.periode("Tilsagnsperiode", tilsagn.periode),
            LabeledDataElement.number("Antall plasser", tilsagn.beregning.input.antallPlasser),
            LabeledDataElement.money("Sats per tiltaksplass per måned", tilsagn.beregning.input.sats),
            LabeledDataElement.money("Totalbeløp", tilsagn.beregning.output.pris),
            LabeledDataElement.money("Gjenstående beløp", tilsagn.gjenstaendeBelop()),
        )

        is TilsagnBeregningPrisPerManedsverk -> listOf(
            LabeledDataElement.periode("Tilsagnsperiode", tilsagn.periode),
            LabeledDataElement.number("Antall plasser", tilsagn.beregning.input.antallPlasser),
            LabeledDataElement.money("Avtalt månedspris per tiltaksplass", tilsagn.beregning.input.sats),
            LabeledDataElement.money("Totalbeløp", tilsagn.beregning.output.pris),
            LabeledDataElement.money("Gjenstående beløp", tilsagn.gjenstaendeBelop()),
        )

        is TilsagnBeregningPrisPerUkesverk -> listOf(
            LabeledDataElement.periode("Tilsagnsperiode", tilsagn.periode),
            LabeledDataElement.number("Antall plasser", tilsagn.beregning.input.antallPlasser),
            LabeledDataElement.money("Avtalt ukespris per tiltaksplass", tilsagn.beregning.input.sats),
            LabeledDataElement.money("Totalbeløp", tilsagn.beregning.output.pris),
            LabeledDataElement.money("Gjenstående beløp", tilsagn.gjenstaendeBelop()),
        )

        is TilsagnBeregningPrisPerHeleUkesverk -> listOf(
            LabeledDataElement.periode("Tilsagnsperiode", tilsagn.periode),
            LabeledDataElement.number("Antall plasser", tilsagn.beregning.input.antallPlasser),
            LabeledDataElement.money("Avtalt ukespris per tiltaksplass", tilsagn.beregning.input.sats),
            LabeledDataElement.money("Totalbeløp", tilsagn.beregning.output.pris),
            LabeledDataElement.money("Gjenstående beløp", tilsagn.gjenstaendeBelop()),
        )

        is TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker -> listOf(
            LabeledDataElement.periode("Tilsagnsperiode", tilsagn.periode),
            LabeledDataElement.number("Antall plasser", tilsagn.beregning.input.antallPlasser),
            LabeledDataElement.money("Pris per time oppfølging", tilsagn.beregning.input.sats),
            LabeledDataElement.money("Totalbeløp", tilsagn.beregning.output.pris),
            LabeledDataElement.money("Gjenstående beløp", tilsagn.gjenstaendeBelop()),
        )
    }
    return DataDetails(entries = entries)
}

@Serializable
data class ArrangorflateTilsagnSummary(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val bestillingsnummer: String,
)
