package no.nav.mulighetsrommet.api.tilsagn.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.tilsagn.model.*
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningHelpers
import no.nav.mulighetsrommet.model.*
import java.math.RoundingMode

@Serializable
data class TilsagnBeregningDto(
    val belop: Int,
    val prismodell: DataDetails,
    val regnestykke: CalculationDto,
) {

    companion object {
        fun from(beregning: TilsagnBeregning): TilsagnBeregningDto {
            return when (beregning) {
                is TilsagnBeregningFri -> TilsagnBeregningDto(
                    belop = beregning.output.belop,
                    prismodell = DataDetails(
                        entries = listOf(
                            DataElement.text("Annen avtalt pris").label("Prismodell"),
                            DataElement.text(beregning.input.prisbetingelser)
                                .label("Pris- og betalingsbetingelser", LabeledDataElementType.MULTILINE),
                        ),
                    ),

                    regnestykke = CalculationDto(
                        breakdown = DataDrivenTableDto(
                            columns = listOf(
                                DataDrivenTableDto.Column(
                                    "beskrivelse",
                                    "Beskrivelse",
                                    sortable = false,
                                ),
                                DataDrivenTableDto.Column(
                                    "belop",
                                    "Beløp",
                                    sortable = false,
                                    align = DataDrivenTableDto.Column.Align.RIGHT,
                                ),
                                DataDrivenTableDto.Column(
                                    "antall",
                                    "Antall",
                                    sortable = false,
                                    align = DataDrivenTableDto.Column.Align.RIGHT,
                                ),
                                DataDrivenTableDto.Column(
                                    "delsum",
                                    "Delsum",
                                    sortable = false,
                                    align = DataDrivenTableDto.Column.Align.RIGHT,
                                ),
                            ),
                            rows = beregning.input.linjer.map { linje ->
                                mapOf(
                                    "beskrivelse" to DataElement.text(linje.beskrivelse.ifEmpty { "<Mangler beskrivelse>" }),
                                    "belop" to DataElement.nok(linje.belop),
                                    "antall" to DataElement.number(linje.antall),
                                    "delsum" to DataElement.nok(linje.belop * linje.antall),
                                )
                            },
                        ),
                    ),
                )

                is TilsagnBeregningFastSatsPerTiltaksplassPerManed -> TilsagnBeregningDto(
                    belop = beregning.output.belop,
                    prismodell = DataDetails(
                        entries = listOf(
                            DataElement.text("Fast sats per tiltaksplass per måned").label("Prismodell"),
                            DataElement.number(beregning.input.antallPlasser).label("Antall plasser"),
                            DataElement.nok(beregning.input.sats).label("Sats"),
                        ),
                    ),

                    regnestykke = getRegnestykkeManedsverk(
                        antallPlasser = beregning.input.antallPlasser,
                        sats = beregning.input.sats,
                        periode = beregning.input.periode,
                        belop = beregning.output.belop,
                    ),
                )

                is TilsagnBeregningPrisPerManedsverk -> TilsagnBeregningDto(
                    belop = beregning.output.belop,
                    prismodell = DataDetails(
                        entries = listOf(
                            DataElement.text("Avtalt månedspris per tiltaksplass").label("Prismodell"),
                            DataElement.number(beregning.input.antallPlasser).label("Antall plasser"),
                            DataElement.nok(beregning.input.sats).label("Avtalt pris"),
                            DataElement.text(beregning.input.prisbetingelser)
                                .label("Pris- og betalingsbetingelser", LabeledDataElementType.MULTILINE),
                        ),
                    ),

                    regnestykke = getRegnestykkeManedsverk(
                        antallPlasser = beregning.input.antallPlasser,
                        sats = beregning.input.sats,
                        periode = beregning.input.periode,
                        belop = beregning.output.belop,
                    ),
                )

                is TilsagnBeregningPrisPerUkesverk -> TilsagnBeregningDto(
                    belop = beregning.output.belop,
                    prismodell = DataDetails(
                        entries = listOf(
                            DataElement.text("Avtalt ukespris per tiltaksplass").label("Prismodell"),
                            DataElement.number(beregning.input.antallPlasser).label("Antall plasser"),
                            DataElement.nok(beregning.input.sats).label("Avtalt pris"),
                            DataElement.text(beregning.input.prisbetingelser)
                                .label("Pris- og betalingsbetingelser", LabeledDataElementType.MULTILINE),
                        ),
                    ),

                    regnestykke = CalculationDto(
                        expression = listOf(
                            DataElement.number(beregning.input.antallPlasser),
                            DataElement.text("plasser"),
                            DataElement.MathOperator(DataElement.MathOperator.Type.MULTIPLY),
                            DataElement.nok(beregning.input.sats),
                            DataElement.text("per tiltaksplass per uke"),
                            DataElement.MathOperator(DataElement.MathOperator.Type.MULTIPLY),
                            DataElement.nok(
                                UtbetalingBeregningHelpers.calculateUkesverk(beregning.input.periode)
                                    .setScale(2, RoundingMode.HALF_UP)
                                    .toDouble(),
                            ),
                            DataElement.text("uker"),
                            DataElement.MathOperator(DataElement.MathOperator.Type.EQUALS),
                            DataElement.nok(beregning.output.belop),
                        ),
                    ),
                )

                is TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker -> TilsagnBeregningDto(
                    belop = beregning.output.belop,
                    prismodell = DataDetails(
                        entries = listOf(
                            DataElement.text("Avtalt pris per time oppfølging per deltaker").label("Prismodell"),
                            DataElement.number(beregning.input.antallPlasser).label("Antall plasser"),
                            DataElement.nok(beregning.input.sats).label("Avtalt pris per oppfølgingstime"),
                            DataElement.number(beregning.input.antallTimerOppfolgingPerDeltaker)
                                .label("Antall oppfølgingstimer per deltaker"),
                            DataElement.text(beregning.input.prisbetingelser)
                                .label("Pris- og betalingsbetingelser", LabeledDataElementType.MULTILINE),
                        ),
                    ),

                    regnestykke = CalculationDto(
                        expression = listOf(
                            DataElement.number(beregning.input.antallPlasser),
                            DataElement.text("plasser"),
                            DataElement.MathOperator(DataElement.MathOperator.Type.MULTIPLY),
                            DataElement.nok(beregning.input.sats),
                            DataElement.text("per oppfølgingstime"),
                            DataElement.MathOperator(DataElement.MathOperator.Type.MULTIPLY),
                            DataElement.nok(beregning.input.antallTimerOppfolgingPerDeltaker),
                            DataElement.text("oppfølgingstimer per deltaker"),
                            DataElement.MathOperator(DataElement.MathOperator.Type.EQUALS),
                            DataElement.nok(beregning.output.belop),
                        ),
                    ),
                )
            }
        }
    }
}

private fun getRegnestykkeManedsverk(
    antallPlasser: Int,
    sats: Int,
    periode: Periode,
    belop: Int,
) = CalculationDto(
    expression = listOf(
        DataElement.number(antallPlasser),
        DataElement.text("plasser"),
        DataElement.MathOperator(DataElement.MathOperator.Type.MULTIPLY),
        DataElement.nok(sats),
        DataElement.text("per tiltaksplass per måned"),
        DataElement.MathOperator(DataElement.MathOperator.Type.MULTIPLY),
        DataElement.number(
            UtbetalingBeregningHelpers.calculateManedsverk(periode)
                .setScale(2, RoundingMode.HALF_UP)
                .toDouble(),
        ),
        DataElement.text("måneder"),
        DataElement.MathOperator(DataElement.MathOperator.Type.EQUALS),
        DataElement.nok(belop),
    ),
)

@Serializable
data class CalculationDto(
    val expression: List<DataElement>? = null,
    val breakdown: DataDrivenTableDto? = null,
)
