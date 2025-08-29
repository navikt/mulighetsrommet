package no.nav.mulighetsrommet.api.tilsagn.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.tilsagn.model.*
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFri.InputLinje
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningHelpers
import no.nav.mulighetsrommet.model.DataDetails
import no.nav.mulighetsrommet.model.DataDrivenTableDto
import no.nav.mulighetsrommet.model.DataElement
import no.nav.mulighetsrommet.model.LabeledDataElementType
import java.math.RoundingMode

@Serializable
sealed class TilsagnBeregningDto {
    abstract val belop: Int
    abstract val prismodell: DataDetails
    abstract val regnestykke: CalculationDto

    @Serializable
    @SerialName("FAST_SATS_PER_TILTAKSPLASS_PER_MANED")
    data class FastSatsPerTiltaksplassPerManed(
        override val belop: Int,
        val sats: Int,
        val antallPlasser: Int,
        val antallManeder: Double,
    ) : TilsagnBeregningDto() {
        override val prismodell = DataDetails(
            entries = listOf(
                DataElement.text("Fast sats per tiltaksplass per måned").label("Prismodell"),
                DataElement.number(antallPlasser).label("Antall plasser"),
                DataElement.nok(sats).label("Sats"),
            ),
        )

        override val regnestykke = CalculationDto(
            expression = listOf(
                DataElement.number(antallPlasser),
                DataElement.text("plasser"),
                DataElement.MathOperator(DataElement.MathOperator.Type.MULTIPLY),
                DataElement.nok(sats),
                DataElement.text("per tiltaksplass per måned"),
                DataElement.MathOperator(DataElement.MathOperator.Type.MULTIPLY),
                DataElement.number(antallManeder),
                DataElement.text("måneder"),
                DataElement.MathOperator(DataElement.MathOperator.Type.EQUALS),
                DataElement.nok(belop),
            ),
        )
    }

    @Serializable
    @SerialName("PRIS_PER_MANEDSVERK")
    data class PrisPerManedsverk(
        override val belop: Int,
        val sats: Int,
        val antallPlasser: Int,
        val antallManeder: Double,
        val prisbetingelser: String?,
    ) : TilsagnBeregningDto() {
        override val prismodell = DataDetails(
            entries = listOf(
                DataElement.text("Avtalt månedspris per tiltaksplass").label("Prismodell"),
                DataElement.number(antallPlasser).label("Antall plasser"),
                DataElement.nok(sats).label("Avtalt pris"),
                DataElement.text(prisbetingelser)
                    .label("Pris- og betalingsbetingelser", LabeledDataElementType.MULTILINE),
            ),
        )

        override val regnestykke = CalculationDto(
            expression = listOf(
                DataElement.number(antallPlasser),
                DataElement.text("plasser"),
                DataElement.MathOperator(DataElement.MathOperator.Type.MULTIPLY),
                DataElement.nok(sats),
                DataElement.text("per tiltaksplass per måned"),
                DataElement.MathOperator(DataElement.MathOperator.Type.MULTIPLY),
                DataElement.number(antallManeder),
                DataElement.text("måneder"),
                DataElement.MathOperator(DataElement.MathOperator.Type.EQUALS),
                DataElement.nok(belop),
            ),
        )
    }

    @Serializable
    @SerialName("PRIS_PER_UKESVERK")
    data class PrisPerUkesverk(
        override val belop: Int,
        val sats: Int,
        val antallPlasser: Int,
        val antallUker: Double,
        val prisbetingelser: String?,
    ) : TilsagnBeregningDto() {
        override val prismodell = DataDetails(
            entries = listOf(
                DataElement.text("Avtalt ukespris per tiltaksplass").label("Prismodell"),
                DataElement.number(antallPlasser).label("Antall plasser"),
                DataElement.nok(sats).label("Avtalt pris"),
                DataElement.text(prisbetingelser)
                    .label("Pris- og betalingsbetingelser", LabeledDataElementType.MULTILINE),
            ),
        )

        override val regnestykke = CalculationDto(
            expression = listOf(
                DataElement.number(antallPlasser),
                DataElement.text("plasser"),
                DataElement.MathOperator(DataElement.MathOperator.Type.MULTIPLY),
                DataElement.nok(sats),
                DataElement.text("per tiltaksplass per uke"),
                DataElement.MathOperator(DataElement.MathOperator.Type.MULTIPLY),
                DataElement.nok(antallUker),
                DataElement.text("uker"),
                DataElement.MathOperator(DataElement.MathOperator.Type.EQUALS),
                DataElement.nok(belop),
            ),
        )
    }

    @Serializable
    @SerialName("PRIS_PER_TIME_OPPFOLGING")
    data class PrisPerTimeOppfolging(
        override val belop: Int,
        val sats: Int,
        val antallPlasser: Int,
        val antallTimerOppfolgingPerDeltaker: Int,
        val prisbetingelser: String?,
    ) : TilsagnBeregningDto() {
        override val prismodell = DataDetails(
            entries = listOf(
                DataElement.text("Avtalt pris per time oppfølging per deltaker").label("Prismodell"),
                DataElement.number(antallPlasser).label("Antall plasser"),
                DataElement.nok(sats).label("Avtalt pris per oppfølgingstime"),
                DataElement.number(antallTimerOppfolgingPerDeltaker).label("Antall oppfølgingstimer per deltaker"),
                DataElement.text(prisbetingelser)
                    .label("Pris- og betalingsbetingelser", LabeledDataElementType.MULTILINE),
            ),
        )

        override val regnestykke = CalculationDto(
            expression = listOf(
                DataElement.number(antallPlasser),
                DataElement.text("plasser"),
                DataElement.MathOperator(DataElement.MathOperator.Type.MULTIPLY),
                DataElement.nok(sats),
                DataElement.text("per oppfølgingstime"),
                DataElement.MathOperator(DataElement.MathOperator.Type.MULTIPLY),
                DataElement.nok(antallTimerOppfolgingPerDeltaker),
                DataElement.text("oppfølgingstimer per deltaker"),
                DataElement.MathOperator(DataElement.MathOperator.Type.EQUALS),
                DataElement.nok(belop),
            ),
        )
    }

    @Serializable
    @SerialName("FRI")
    data class Fri(
        override val belop: Int,
        val linjer: List<InputLinje>,
        val prisbetingelser: String?,
    ) : TilsagnBeregningDto() {
        override val prismodell = DataDetails(
            entries = listOf(
                DataElement.text("Annen avtalt pris").label("Prismodell"),
                DataElement.text(prisbetingelser)
                    .label("Pris- og betalingsbetingelser", LabeledDataElementType.MULTILINE),
            ),
        )

        override val regnestykke = CalculationDto(
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
                rows = linjer.map { linje ->
                    mapOf(
                        "beskrivelse" to DataElement.text(linje.beskrivelse.ifEmpty { "<Mangler beskrivelse>" }),
                        "belop" to DataElement.nok(linje.belop),
                        "antall" to DataElement.number(linje.antall),
                        "delsum" to DataElement.nok(linje.belop * linje.antall),
                    )
                },
            ),
        )
    }

    companion object {
        fun from(beregning: TilsagnBeregning): TilsagnBeregningDto {
            return when (beregning) {
                is TilsagnBeregningFri -> Fri(
                    belop = beregning.output.belop,
                    linjer = beregning.input.linjer,
                    prisbetingelser = beregning.input.prisbetingelser,
                )

                is TilsagnBeregningFastSatsPerTiltaksplassPerManed -> FastSatsPerTiltaksplassPerManed(
                    belop = beregning.output.belop,
                    antallPlasser = beregning.input.antallPlasser,
                    sats = beregning.input.sats,
                    antallManeder = UtbetalingBeregningHelpers.calculateManedsverk(beregning.input.periode)
                        .setScale(2, RoundingMode.HALF_UP)
                        .toDouble(),
                )

                is TilsagnBeregningPrisPerManedsverk -> PrisPerManedsverk(
                    belop = beregning.output.belop,
                    antallPlasser = beregning.input.antallPlasser,
                    sats = beregning.input.sats,
                    prisbetingelser = beregning.input.prisbetingelser,
                    antallManeder = UtbetalingBeregningHelpers.calculateManedsverk(beregning.input.periode)
                        .setScale(2, RoundingMode.HALF_UP)
                        .toDouble(),
                )

                is TilsagnBeregningPrisPerUkesverk -> PrisPerUkesverk(
                    belop = beregning.output.belop,
                    antallPlasser = beregning.input.antallPlasser,
                    sats = beregning.input.sats,
                    prisbetingelser = beregning.input.prisbetingelser,
                    antallUker = UtbetalingBeregningHelpers.calculateUkesverk(beregning.input.periode)
                        .setScale(2, RoundingMode.HALF_UP)
                        .toDouble(),
                )

                is TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker -> PrisPerTimeOppfolging(
                    belop = beregning.output.belop,
                    sats = beregning.input.sats,
                    antallPlasser = beregning.input.antallPlasser,
                    antallTimerOppfolgingPerDeltaker = beregning.input.antallTimerOppfolgingPerDeltaker,
                    prisbetingelser = beregning.input.prisbetingelser,
                )
            }
        }
    }
}

@Serializable
data class CalculationDto(
    val expression: List<DataElement>? = null,
    val breakdown: DataDrivenTableDto? = null,
)
