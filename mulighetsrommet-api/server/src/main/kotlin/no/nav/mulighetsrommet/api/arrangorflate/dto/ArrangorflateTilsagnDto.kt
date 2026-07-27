package no.nav.mulighetsrommet.api.arrangorflate.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.arrangorflate.service.ArrangorflatePersonalia
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningAnnenAvtaltPris
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningAvtaltPrisPerBenyttetPlassPerHeleUke
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningAvtaltPrisPerBenyttetPlassPerManed
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningAvtaltPrisPerBenyttetPlassPerUke
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningAvtaltPrisPerTimeOppfolgingPerDeltaker
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFastSatsPerBenyttetPlassPerManed
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.model.DataDetails
import no.nav.mulighetsrommet.model.LabeledDataElement
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
    val deltakere: List<ArrangorflatePersonalia>,
) {

    companion object {
        fun from(tilsagn: Tilsagn, deltakere: List<ArrangorflatePersonalia>) = ArrangorflateTilsagnDto(
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
        is TilsagnBeregningAnnenAvtaltPris -> listOf(
            LabeledDataElement.periode("Tilsagnsperiode", tilsagn.periode),
            LabeledDataElement.money("Totalbeløp", tilsagn.beregning.output.pris),
            LabeledDataElement.money("Gjenstående beløp", tilsagn.gjenstaendeBelop()),
        )

        is TilsagnBeregningFastSatsPerBenyttetPlassPerManed -> listOf(
            LabeledDataElement.periode("Tilsagnsperiode", tilsagn.periode),
            LabeledDataElement.number("Antall plasser", tilsagn.beregning.input.antallPlasser),
            LabeledDataElement.money("Sats per tiltaksplass per måned", tilsagn.beregning.input.sats),
            LabeledDataElement.money("Totalbeløp", tilsagn.beregning.output.pris),
            LabeledDataElement.money("Gjenstående beløp", tilsagn.gjenstaendeBelop()),
        )

        is TilsagnBeregningAvtaltPrisPerBenyttetPlassPerManed -> listOf(
            LabeledDataElement.periode("Tilsagnsperiode", tilsagn.periode),
            LabeledDataElement.number("Antall plasser", tilsagn.beregning.input.antallPlasser),
            LabeledDataElement.money("Avtalt månedspris per tiltaksplass", tilsagn.beregning.input.sats),
            LabeledDataElement.money("Totalbeløp", tilsagn.beregning.output.pris),
            LabeledDataElement.money("Gjenstående beløp", tilsagn.gjenstaendeBelop()),
        )

        is TilsagnBeregningAvtaltPrisPerBenyttetPlassPerUke -> listOf(
            LabeledDataElement.periode("Tilsagnsperiode", tilsagn.periode),
            LabeledDataElement.number("Antall plasser", tilsagn.beregning.input.antallPlasser),
            LabeledDataElement.money("Avtalt ukespris per tiltaksplass", tilsagn.beregning.input.sats),
            LabeledDataElement.money("Totalbeløp", tilsagn.beregning.output.pris),
            LabeledDataElement.money("Gjenstående beløp", tilsagn.gjenstaendeBelop()),
        )

        is TilsagnBeregningAvtaltPrisPerBenyttetPlassPerHeleUke -> listOf(
            LabeledDataElement.periode("Tilsagnsperiode", tilsagn.periode),
            LabeledDataElement.number("Antall plasser", tilsagn.beregning.input.antallPlasser),
            LabeledDataElement.money("Avtalt ukespris per tiltaksplass", tilsagn.beregning.input.sats),
            LabeledDataElement.money("Totalbeløp", tilsagn.beregning.output.pris),
            LabeledDataElement.money("Gjenstående beløp", tilsagn.gjenstaendeBelop()),
        )

        is TilsagnBeregningAvtaltPrisPerTimeOppfolgingPerDeltaker -> listOf(
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
