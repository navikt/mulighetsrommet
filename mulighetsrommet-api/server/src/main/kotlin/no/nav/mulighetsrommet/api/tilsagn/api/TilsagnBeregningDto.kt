package no.nav.mulighetsrommet.api.tilsagn.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.tiltak.PrismodellType
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregning
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningAnnenAvtaltPris
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningAvtaltPrisPerBenyttetPlassPerHeleUke
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningAvtaltPrisPerBenyttetPlassPerManed
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningAvtaltPrisPerBenyttetPlassPerUke
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningAvtaltPrisPerTimeOppfolgingPerDeltaker
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFastSatsPerBenyttetPlassPerManed
import no.nav.mulighetsrommet.api.utbetaling.model.StengtPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningHelpers
import no.nav.mulighetsrommet.model.DataDetails
import no.nav.mulighetsrommet.model.DataDrivenTableDto
import no.nav.mulighetsrommet.model.DataElement
import no.nav.mulighetsrommet.model.LabeledDataElementType
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.ValutaBelop

@Serializable
data class TilsagnBeregningDto(
    val pris: ValutaBelop,
    val prismodell: DataDetails,
    val regnestykke: CalculationDto,
    val stengt: List<StengtPeriode>,
) {
    companion object {
        fun from(beregning: TilsagnBeregning): TilsagnBeregningDto {
            return when (beregning) {
                is TilsagnBeregningAnnenAvtaltPris -> TilsagnBeregningDto(
                    pris = beregning.output.pris,
                    prismodell = DataDetails(
                        entries = listOf(
                            DataElement.text(PrismodellType.ANNEN_AVTALT_PRIS.navn).label("Prismodell"),
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
                                DataDrivenTableDto.Row(
                                    cells = mapOf(
                                        "beskrivelse" to DataElement.text(linje.beskrivelse.ifEmpty { "<Mangler beskrivelse>" }),
                                        "belop" to DataElement.money(
                                            linje.pris,
                                        ),
                                        "antall" to DataElement.number(linje.antall),
                                        "delsum" to DataElement.money(
                                            ValutaBelop(
                                                linje.pris.belop * linje.antall,
                                                linje.pris.valuta,
                                            ),
                                        ),
                                    ),
                                )
                            },
                        ),
                    ),
                    stengt = listOf(),
                )

                is TilsagnBeregningFastSatsPerBenyttetPlassPerManed -> TilsagnBeregningDto(
                    pris = beregning.output.pris,
                    prismodell = DataDetails(
                        entries = listOf(
                            DataElement.text(PrismodellType.FAST_SATS_PER_BENYTTET_PLASS_PER_MANED.navn)
                                .label("Prismodell"),
                            DataElement.number(beregning.input.antallPlasser).label("Antall plasser"),
                            DataElement.money(
                                beregning.input.sats,
                            ).label("Sats"),
                        ),
                    ),
                    regnestykke = getRegnestykkeManedsverk(
                        antallPlasser = beregning.input.antallPlasser,
                        sats = beregning.input.sats,
                        periode = beregning.input.periode,
                        stengt = beregning.input.stengt,
                        sum = beregning.output.pris,
                    ),
                    stengt = getStengtePerioder(beregning.input.stengt),
                )

                is TilsagnBeregningAvtaltPrisPerBenyttetPlassPerManed -> TilsagnBeregningDto(
                    pris = beregning.output.pris,
                    prismodell = DataDetails(
                        entries = listOf(
                            DataElement.text(PrismodellType.AVTALT_PRIS_PER_BENYTTET_PLASS_PER_MANED.navn)
                                .label("Prismodell"),
                            DataElement.number(beregning.input.antallPlasser).label("Antall plasser"),
                            DataElement.money(beregning.input.sats).label("Avtalt pris"),
                            DataElement.text(beregning.input.prisbetingelser)
                                .label("Pris- og betalingsbetingelser", LabeledDataElementType.MULTILINE),
                        ),
                    ),

                    regnestykke = getRegnestykkeManedsverk(
                        antallPlasser = beregning.input.antallPlasser,
                        sats = beregning.input.sats,
                        periode = beregning.input.periode,
                        stengt = beregning.input.stengt,
                        sum = beregning.output.pris,
                    ),

                    stengt = getStengtePerioder(beregning.input.stengt),
                )

                is TilsagnBeregningAvtaltPrisPerBenyttetPlassPerUke -> TilsagnBeregningDto(
                    pris = beregning.output.pris,
                    prismodell = DataDetails(
                        entries = listOf(
                            DataElement.text(PrismodellType.AVTALT_PRIS_PER_BENYTTET_PLASS_PER_UKE.navn)
                                .label("Prismodell"),
                            DataElement.number(beregning.input.antallPlasser).label("Antall plasser"),
                            DataElement.money(beregning.input.sats).label("Avtalt pris"),
                            DataElement.text(beregning.input.prisbetingelser)
                                .label("Pris- og betalingsbetingelser", LabeledDataElementType.MULTILINE),
                        ),
                    ),

                    regnestykke = CalculationDto(
                        expression = listOf(
                            DataElement.number(beregning.input.antallPlasser),
                            DataElement.text("plasser"),
                            DataElement.MathOperator(DataElement.MathOperator.Type.MULTIPLY),
                            DataElement.money(beregning.input.sats),
                            DataElement.text("per tiltaksplass per uke"),
                            DataElement.MathOperator(DataElement.MathOperator.Type.MULTIPLY),
                            DataElement.number(
                                beregning.input.periode
                                    .subtractPeriods(beregning.input.stengt.map { it.periode })
                                    .sumOf { UtbetalingBeregningHelpers.calculateWeeksInPeriode(it) }
                                    .toDouble(),
                            ),
                            DataElement.text("uker"),
                            DataElement.MathOperator(DataElement.MathOperator.Type.EQUALS),
                            DataElement.money(
                                beregning.output.pris,
                            ),
                        ),
                    ),

                    stengt = getStengtePerioder(beregning.input.stengt),
                )

                is TilsagnBeregningAvtaltPrisPerBenyttetPlassPerHeleUke -> TilsagnBeregningDto(
                    pris = beregning.output.pris,
                    prismodell = DataDetails(
                        entries = listOf(
                            DataElement.text(PrismodellType.AVTALT_PRIS_PER_BENYTTET_PLASS_PER_HELE_UKE.navn)
                                .label("Prismodell"),
                            DataElement.number(beregning.input.antallPlasser).label("Antall plasser"),
                            DataElement.money(beregning.input.sats).label("Avtalt pris"),
                            DataElement.text(beregning.input.prisbetingelser)
                                .label("Pris- og betalingsbetingelser", LabeledDataElementType.MULTILINE),
                        ),
                    ),

                    regnestykke = CalculationDto(
                        expression = listOf(
                            DataElement.number(beregning.input.antallPlasser),
                            DataElement.text("plasser"),
                            DataElement.MathOperator(DataElement.MathOperator.Type.MULTIPLY),
                            DataElement.money(beregning.input.sats),
                            DataElement.text("per tiltaksplass per uke"),
                            DataElement.MathOperator(DataElement.MathOperator.Type.MULTIPLY),
                            DataElement.number(
                                beregning.input.periode
                                    .subtractPeriods(beregning.input.stengt.map { it.periode })
                                    .sumOf { UtbetalingBeregningHelpers.calculateWholeWeeksInPeriode(it) }
                                    .toDouble(),
                            ),
                            DataElement.text("uker"),
                            DataElement.MathOperator(DataElement.MathOperator.Type.EQUALS),
                            DataElement.money(beregning.output.pris),
                        ),
                    ),

                    stengt = getStengtePerioder(beregning.input.stengt),
                )

                is TilsagnBeregningAvtaltPrisPerTimeOppfolgingPerDeltaker -> TilsagnBeregningDto(
                    pris = beregning.output.pris,
                    prismodell = DataDetails(
                        entries = listOf(
                            DataElement.text(PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER.navn)
                                .label("Prismodell"),
                            DataElement.number(beregning.input.antallPlasser).label("Antall plasser"),
                            DataElement.money(beregning.input.sats).label("Avtalt pris per oppfølgingstime"),
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
                            DataElement.money(beregning.input.sats),
                            DataElement.text("per oppfølgingstime"),
                            DataElement.MathOperator(DataElement.MathOperator.Type.MULTIPLY),
                            DataElement.number(beregning.input.antallTimerOppfolgingPerDeltaker),
                            DataElement.text("oppfølgingstimer per deltaker"),
                            DataElement.MathOperator(DataElement.MathOperator.Type.EQUALS),
                            DataElement.money(beregning.output.pris),
                        ),
                    ),

                    stengt = listOf(),
                )
            }
        }
    }
}

private fun getRegnestykkeManedsverk(
    antallPlasser: Int,
    sats: ValutaBelop,
    periode: Periode,
    stengt: Set<StengtPeriode>,
    sum: ValutaBelop,
): CalculationDto {
    val aktivePerioder = periode.subtractPeriods(stengt.map { it.periode })
    val totalMonths = aktivePerioder
        .sumOf { UtbetalingBeregningHelpers.calculateMonthsInPeriode(it) }
        .toDouble()

    return CalculationDto(
        expression = listOf(
            DataElement.number(antallPlasser),
            DataElement.text("plasser"),
            DataElement.MathOperator(DataElement.MathOperator.Type.MULTIPLY),
            DataElement.money(sats),
            DataElement.text("per tiltaksplass per måned"),
            DataElement.MathOperator(DataElement.MathOperator.Type.MULTIPLY),
            DataElement.number(totalMonths),
            DataElement.text("måneder"),
            DataElement.MathOperator(DataElement.MathOperator.Type.EQUALS),
            DataElement.money(sum),
        ),
    )
}

private fun getStengtePerioder(
    perioder: Set<StengtPeriode>,
): List<StengtPeriode> = perioder.sortedBy { it.periode }

@Serializable
data class CalculationDto(
    val expression: List<DataElement>? = null,
    val breakdown: DataDrivenTableDto? = null,
)
