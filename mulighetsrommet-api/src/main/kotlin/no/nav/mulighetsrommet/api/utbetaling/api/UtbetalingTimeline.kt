package no.nav.mulighetsrommet.api.utbetaling.api

import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelsesprosentPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.StengtPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningOutputDeltakelse
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.TimelineDto
import no.nav.mulighetsrommet.model.TimelineDto.Row.Period
import no.nav.mulighetsrommet.model.TimelineDto.Row.Period.Variant
import no.nav.mulighetsrommet.model.ValutaBelop
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.text.NumberFormat
import java.util.Locale

object UtbetalingTimeline {
    val formatter: NumberFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag("no-NO"))
    val formatCurrency = { pris: ValutaBelop -> "${formatter.format(pris.belop)} ${pris.valuta.name}" }
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    fun deltakelseTimeline(
        periode: Periode,
        stengt: List<StengtPeriode>,
        beregningRow: TimelineDto.Row,
    ): TimelineDto {
        return TimelineDto(
            startDate = periode.start,
            endDate = periode.getLastInclusiveDate(),
            rows = listOfNotNull(
                if (stengt.isNotEmpty()) {
                    TimelineDto.Row(
                        periods = stengt.mapIndexed { index, it ->
                            Period(
                                start = it.periode.start,
                                key = index.toString(),
                                end = it.periode.getLastInclusiveDate(),
                                status = Variant.WARNING,
                                content = "",
                                hover = """
                                    Periode: ${it.periode.start.formaterDatoTilEuropeiskDatoformat()} -
                                             ${it.periode.getLastInclusiveDate().formaterDatoTilEuropeiskDatoformat()}
                                """.trimIndent(),
                            )
                        },
                        label = "Stengt hos arrangør",
                    )
                } else {
                    null
                },
                beregningRow,
            ),
        )
    }

    fun ukesverkBeregningRow(
        beregningOutput: UtbetalingBeregningOutputDeltakelse,
    ) = TimelineDto.Row(
        label = "Beregning",
        periods = beregningOutput.perioder.mapIndexed { index, it ->
            Period(
                start = it.periode.start,
                key = index.toString(),
                end = it.periode.getLastInclusiveDate(),
                status = Variant.INFO,
                content = "Pris per uke: ${formatCurrency(it.sats)}, Ukesverk: ${formatter.format(it.faktor)}",
                hover = """
                    Pris per uke: ${formatCurrency(it.sats)},
                    Ukesverk: ${formatter.format(it.faktor)},
                    Periode: ${it.periode.start.formaterDatoTilEuropeiskDatoformat()} -
                        ${it.periode.getLastInclusiveDate().formaterDatoTilEuropeiskDatoformat()}
                """.trimIndent(),
            )
        },
    )

    fun manedsverkBeregningRow(
        beregningOutput: UtbetalingBeregningOutputDeltakelse,
    ) = TimelineDto.Row(
        label = "Beregning",
        periods = beregningOutput.perioder.mapIndexed { index, it ->
            Period(
                start = it.periode.start,
                key = index.toString(),
                end = it.periode.getLastInclusiveDate(),
                status = Variant.INFO,
                content = "Pris per måned: ${formatCurrency(it.sats)}, Månedsverk: ${formatter.format(it.faktor)}",
                hover = """
                    Pris per måned: ${formatCurrency(it.sats)},
                    Månedsverk: ${formatter.format(it.faktor)},
                    Periode: ${it.periode.start.formaterDatoTilEuropeiskDatoformat()} -
                        ${it.periode.getLastInclusiveDate().formaterDatoTilEuropeiskDatoformat()}
                """.trimIndent(),
            )
        },
    )

    fun fastSatsPerTiltaksplassPerManedRow(
        beregningOutput: UtbetalingBeregningOutputDeltakelse,
        deltakelsesprosenter: List<DeltakelsesprosentPeriode>,
    ) = TimelineDto.Row(
        label = "Beregning",
        periods = beregningOutput.perioder.mapIndexed { index, beregnetPeriode ->
            val deltakelsesprosent = deltakelsesprosenter
                .find { it.periode.contains(beregnetPeriode.periode) }
                ?.deltakelsesprosent
            if (deltakelsesprosent == null) {
                log.warn("Deltakesesprosent var null! deltakerId: ${beregningOutput.deltakelseId}, beregnetPeriode: $beregnetPeriode")
            }
            Period(
                start = beregnetPeriode.periode.start,
                key = index.toString(),
                end = beregnetPeriode.periode.getLastInclusiveDate(),
                status = Variant.INFO,
                content = "Deltakesesprosent: ${deltakelsesprosent?.let { formatter.format(it) } ?: "null" }, Månedsverk: ${formatter.format(beregnetPeriode.faktor)}",
                hover = """
                    Deltakesesprosent: ${deltakelsesprosent?.let { formatter.format(it) } ?: "null" },
                    Månedsverk: ${formatter.format(beregnetPeriode.faktor)},
                    Periode: ${beregnetPeriode.periode.start.formaterDatoTilEuropeiskDatoformat()} -
                        ${beregnetPeriode.periode.getLastInclusiveDate().formaterDatoTilEuropeiskDatoformat()}
                """.trimIndent(),
            )
        },
    )
}
