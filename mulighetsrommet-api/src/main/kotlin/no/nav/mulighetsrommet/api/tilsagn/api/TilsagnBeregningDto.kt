package no.nav.mulighetsrommet.api.tilsagn.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregning
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFastSatsPerTiltaksplassPerManed
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFri
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningPrisPerHeleUkesverk
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningPrisPerManedsverk
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningPrisPerUkesverk
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningHelpers
import no.nav.mulighetsrommet.model.Currency
import no.nav.mulighetsrommet.model.DataDetails
import no.nav.mulighetsrommet.model.DataDrivenTableDto
import no.nav.mulighetsrommet.model.DataElement
import no.nav.mulighetsrommet.model.LabeledDataElementType
import no.nav.mulighetsrommet.model.Periode

@Serializable
data class TilsagnBeregningDto(
    val belop: Int,
    val valuta: Currency,
    val prismodell: DataDetails,
    val regnestykke: CalculationDto,
) {
    companion object {
        fun from(beregning: TilsagnBeregning): TilsagnBeregningDto {
            return when (beregning) {
                is TilsagnBeregningFri -> TilsagnBeregningDto(
                    belop = beregning.output.belop,
                    valuta = beregning.output.valuta,
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
                                        "belop" to DataElement.currency(
                                            linje.belop,
                                            DataElement.CurrencyValue.Currency.from(linje.valuta),
                                        ),
                                        "antall" to DataElement.number(linje.antall),
                                        "delsum" to DataElement.currency(
                                            linje.belop * linje.antall,
                                            DataElement.CurrencyValue.Currency.from(linje.valuta),
                                        ),
                                    ),
                                )
                            },
                        ),
                    ),
                )

                is TilsagnBeregningFastSatsPerTiltaksplassPerManed -> TilsagnBeregningDto(
                    belop = beregning.output.belop,
                    valuta = beregning.output.valuta,
                    prismodell = DataDetails(
                        entries = listOf(
                            DataElement.text(PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK.navn)
                                .label("Prismodell"),
                            DataElement.number(beregning.input.antallPlasser).label("Antall plasser"),
                            DataElement.currency(
                                beregning.input.sats,
                                DataElement.CurrencyValue.Currency.from(beregning.input.valuta),
                            ).label("Sats"),
                        ),
                    ),
                    regnestykke = getRegnestykkeManedsverk(
                        antallPlasser = beregning.input.antallPlasser,
                        sats = beregning.input.sats,
                        valuta = beregning.input.valuta,
                        periode = beregning.input.periode,
                        belop = beregning.output.belop,
                    ),
                )

                is TilsagnBeregningPrisPerManedsverk -> TilsagnBeregningDto(
                    belop = beregning.output.belop,
                    valuta = beregning.output.valuta,
                    prismodell = DataDetails(
                        entries = listOf(
                            DataElement.text(PrismodellType.AVTALT_PRIS_PER_MANEDSVERK.navn).label("Prismodell"),
                            DataElement.number(beregning.input.antallPlasser).label("Antall plasser"),
                            DataElement.currency(
                                beregning.input.sats,
                                DataElement.CurrencyValue.Currency.from(beregning.input.valuta),
                            ).label("Avtalt pris"),
                            DataElement.text(beregning.input.prisbetingelser)
                                .label("Pris- og betalingsbetingelser", LabeledDataElementType.MULTILINE),
                        ),
                    ),

                    regnestykke = getRegnestykkeManedsverk(
                        antallPlasser = beregning.input.antallPlasser,
                        sats = beregning.input.sats,
                        valuta = beregning.input.valuta,
                        periode = beregning.input.periode,
                        belop = beregning.output.belop,
                    ),
                )

                is TilsagnBeregningPrisPerUkesverk -> TilsagnBeregningDto(
                    belop = beregning.output.belop,
                    valuta = beregning.output.valuta,
                    prismodell = DataDetails(
                        entries = listOf(
                            DataElement.text(PrismodellType.AVTALT_PRIS_PER_UKESVERK.navn).label("Prismodell"),
                            DataElement.number(beregning.input.antallPlasser).label("Antall plasser"),
                            DataElement.currency(
                                beregning.input.sats,
                                DataElement.CurrencyValue.Currency.from(beregning.input.valuta),
                            ).label("Avtalt pris"),
                            DataElement.text(beregning.input.prisbetingelser)
                                .label("Pris- og betalingsbetingelser", LabeledDataElementType.MULTILINE),
                        ),
                    ),

                    regnestykke = CalculationDto(
                        expression = listOf(
                            DataElement.number(beregning.input.antallPlasser),
                            DataElement.text("plasser"),
                            DataElement.MathOperator(DataElement.MathOperator.Type.MULTIPLY),
                            DataElement.currency(
                                beregning.input.sats,
                                DataElement.CurrencyValue.Currency.from(beregning.input.valuta),
                            ),
                            DataElement.text("per tiltaksplass per uke"),
                            DataElement.MathOperator(DataElement.MathOperator.Type.MULTIPLY),
                            DataElement.number(
                                UtbetalingBeregningHelpers.calculateWeeksInPeriode(beregning.input.periode).toDouble(),
                            ),
                            DataElement.text("uker"),
                            DataElement.MathOperator(DataElement.MathOperator.Type.EQUALS),
                            DataElement.currency(
                                beregning.output.belop,
                                DataElement.CurrencyValue.Currency.from(beregning.input.valuta),
                            ),
                        ),
                    ),
                )

                is TilsagnBeregningPrisPerHeleUkesverk -> TilsagnBeregningDto(
                    belop = beregning.output.belop,
                    valuta = beregning.output.valuta,
                    prismodell = DataDetails(
                        entries = listOf(
                            DataElement.text(PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK.navn).label("Prismodell"),
                            DataElement.number(beregning.input.antallPlasser).label("Antall plasser"),
                            DataElement.currency(beregning.input.sats, DataElement.CurrencyValue.Currency.from(beregning.input.valuta)).label("Avtalt pris"),
                            DataElement.text(beregning.input.prisbetingelser)
                                .label("Pris- og betalingsbetingelser", LabeledDataElementType.MULTILINE),
                        ),
                    ),

                    regnestykke = CalculationDto(
                        expression = listOf(
                            DataElement.number(beregning.input.antallPlasser),
                            DataElement.text("plasser"),
                            DataElement.MathOperator(DataElement.MathOperator.Type.MULTIPLY),
                            DataElement.currency(
                                beregning.input.sats,
                                DataElement.CurrencyValue.Currency.from(beregning.input.valuta),
                            ),
                            DataElement.text("per tiltaksplass per uke"),
                            DataElement.MathOperator(DataElement.MathOperator.Type.MULTIPLY),
                            DataElement.number(
                                UtbetalingBeregningHelpers.calculateWholeWeeksInPeriode(beregning.input.periode)
                                    .toDouble(),
                            ),
                            DataElement.text("uker"),
                            DataElement.MathOperator(DataElement.MathOperator.Type.EQUALS),
                            DataElement.currency(
                                beregning.output.belop,
                                DataElement.CurrencyValue.Currency.from(beregning.input.valuta),
                            ),
                        ),
                    ),
                )

                is TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker -> TilsagnBeregningDto(
                    belop = beregning.output.belop,
                    valuta = beregning.output.valuta,
                    prismodell = DataDetails(
                        entries = listOf(
                            DataElement.text(PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER.navn)
                                .label("Prismodell"),
                            DataElement.number(beregning.input.antallPlasser).label("Antall plasser"),
                            DataElement.currency(
                                beregning.input.sats,
                                DataElement.CurrencyValue.Currency.from(beregning.input.valuta),
                            ).label("Avtalt pris per oppfølgingstime"),
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
                            DataElement.currency(
                                beregning.input.sats,
                                DataElement.CurrencyValue.Currency.from(beregning.input.valuta),
                            ),
                            DataElement.text("per oppfølgingstime"),
                            DataElement.MathOperator(DataElement.MathOperator.Type.MULTIPLY),
                            DataElement.number(beregning.input.antallTimerOppfolgingPerDeltaker),
                            DataElement.text("oppfølgingstimer per deltaker"),
                            DataElement.MathOperator(DataElement.MathOperator.Type.EQUALS),
                            DataElement.currency(
                                beregning.output.belop,
                                DataElement.CurrencyValue.Currency.from(beregning.input.valuta),
                            ),
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
    valuta: Currency,
    periode: Periode,
    belop: Int,
) = CalculationDto(
    expression = listOf(
        DataElement.number(antallPlasser),
        DataElement.text("plasser"),
        DataElement.MathOperator(DataElement.MathOperator.Type.MULTIPLY),
        DataElement.currency(sats, DataElement.CurrencyValue.Currency.from(valuta)),
        DataElement.text("per tiltaksplass per måned"),
        DataElement.MathOperator(DataElement.MathOperator.Type.MULTIPLY),
        DataElement.number(
            UtbetalingBeregningHelpers.calculateMonthsInPeriode(periode).toDouble(),
        ),
        DataElement.text("måneder"),
        DataElement.MathOperator(DataElement.MathOperator.Type.EQUALS),
        DataElement.currency(belop, DataElement.CurrencyValue.Currency.from(valuta)),
    ),
)

@Serializable
data class CalculationDto(
    val expression: List<DataElement>? = null,
    val breakdown: DataDrivenTableDto? = null,
)
