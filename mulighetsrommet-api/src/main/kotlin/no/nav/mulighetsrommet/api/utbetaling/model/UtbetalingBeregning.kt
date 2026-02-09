package no.nav.mulighetsrommet.api.utbetaling.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.model.withValuta
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.UUID

@Serializable
sealed class UtbetalingBeregning {
    abstract val input: UtbetalingBeregningInput
    abstract val output: UtbetalingBeregningOutput

    fun getDigest(): String {
        return (input.hashCode() + output.hashCode()).toHexString()
    }

    open fun deltakelsePerioder(): Set<DeltakelsePeriode> {
        return output.deltakelser().map {
            DeltakelsePeriode(it.deltakelseId, it.periode())
        }.toSet()
    }
}

sealed class UtbetalingBeregningInput {
    abstract fun deltakelser(): Set<UtbetalingBeregningInputDeltakelse>
}

sealed class UtbetalingBeregningInputDeltakelse {
    abstract val deltakelseId: UUID
    abstract fun periode(): Periode
}

sealed class UtbetalingBeregningOutput {
    abstract val pris: ValutaBelop
    abstract fun deltakelser(): Set<UtbetalingBeregningOutputDeltakelse>
}

@Serializable
data class UtbetalingBeregningOutputDeltakelse(
    @Serializable(with = UUIDSerializer::class)
    val deltakelseId: UUID,
    val perioder: Set<BeregnetPeriode>,
) {
    fun periode(): Periode = Periode.fromRange(perioder.map { it.periode })

    @Serializable
    data class BeregnetPeriode(
        val periode: Periode,
        val faktor: Double,
        val sats: ValutaBelop,
    )
}

@Serializable
data class SatsPeriode(
    val periode: Periode,
    val sats: ValutaBelop,
)

@Serializable
data class StengtPeriode(
    val periode: Periode,
    val beskrivelse: String,
)

@Serializable
data class DeltakelseDeltakelsesprosentPerioder(
    @Serializable(with = UUIDSerializer::class)
    override val deltakelseId: UUID,
    val perioder: List<DeltakelsesprosentPeriode>,
) : UtbetalingBeregningInputDeltakelse() {
    override fun periode() = Periode.fromRange(perioder.map { it.periode })
}

@Serializable
data class DeltakelsesprosentPeriode(
    val periode: Periode,
    val deltakelsesprosent: Double,
)

@Serializable
data class DeltakelsePeriode(
    @Serializable(with = UUIDSerializer::class)
    override val deltakelseId: UUID,
    val periode: Periode,
) : UtbetalingBeregningInputDeltakelse() {
    override fun periode() = periode
}

object UtbetalingBeregningHelpers {
    /**
     * Presisjon underveis for å oppnå en god beregning av totalbeløpet.
     */
    const val CALCULATION_PRECISION = 20

    /**
     * Presisjon for den delen av beregningen som blir med i output og tilgjengelig for innsyn, test, etc.
     */
    const val OUTPUT_PRECISION = 5

    /**
     * Fra og med denne datoen:
     * - Helgedager telles ikke med i beregningen av månedsverk (antall dager deltatt innenfor en måned)
     *
     * Før denne datoen:
     * - Helgedager telles med i beregningen av månedsverk (antall dager deltatt innenfor en måned)
     */
    private val DELTAKELSE_MONTHS_FRACTION_VERSION_2_DATE: LocalDate = LocalDate.of(2025, 8, 1)

    /**
     * Fra og med denne datoen:
     * - Deltakelsesmengder på 50% eller mindre regnes som et halvt månedsverk.
     * - Deltakelsesmengder på mer enn 50% regnes som et fullt månedsverk.
     * - Gjelder bare for AFT-deltakelser, VTA-deltakelser har samme beregning som før denne datoen.
     *
     * Før denne datoen:
     * - Deltakelsesmengder på 50% eller mer regnes som et fullt månedsverk.
     * - Deltakelsesmengder på mindre enn 50% regnes som et halvt månedsverk.
     * - Gjelder både AFT- og VTA-deltakelser.
     */
    private val DELTAKELSE_MONTHS_FRACTION_VERSION_3_DATE: LocalDate = LocalDate.of(2026, 1, 1)

    fun calculateDeltakelseManedsverkForDeltakelsesprosent(
        tiltakskode: Tiltakskode,
        deltakelse: DeltakelseDeltakelsesprosentPerioder,
        satser: Set<SatsPeriode>,
        stengtHosArrangor: List<Periode>,
    ): UtbetalingBeregningOutputDeltakelse {
        val perioder = deltakelse.perioder.flatMap { deltakelsePeriode ->
            deltakelsePeriode.periode
                .subtractPeriods(stengtHosArrangor)
                .map { DeltakelsesprosentPeriode(it, deltakelsePeriode.deltakelsesprosent) }
        }
        return calculateDeltakelseOutput(deltakelse.deltakelseId, perioder.map { it.periode }, satser) { periode ->
            val fraction = getMonthsFraction(periode)
            val prosent = perioder.first { it.periode == periode }.deltakelsesprosent
            applyDeltakelsesprosent(tiltakskode, periode, fraction, prosent)
        }
    }

    fun calculateDeltakelseManedsverk(
        deltakelse: DeltakelsePeriode,
        satser: Set<SatsPeriode>,
        stengtHosArrangor: List<Periode>,
    ): UtbetalingBeregningOutputDeltakelse {
        val perioder = deltakelse.periode.subtractPeriods(stengtHosArrangor)
        return calculateDeltakelseOutput(deltakelse.deltakelseId, perioder, satser) { periode ->
            getMonthsFraction(periode)
        }
    }

    fun calculateDeltakelseUkesverk(
        deltakelse: DeltakelsePeriode,
        satser: Set<SatsPeriode>,
        stengtHosArrangor: List<Periode>,
    ): UtbetalingBeregningOutputDeltakelse {
        val perioder = deltakelse.periode.subtractPeriods(stengtHosArrangor)
        return calculateDeltakelseOutput(deltakelse.deltakelseId, perioder, satser) { periode ->
            getWeeksFraction(periode)
        }
    }

    fun calculateDeltakelseHeleUkesverk(
        deltakelse: DeltakelsePeriode,
        satser: Set<SatsPeriode>,
        stengtHosArrangor: List<Periode>,
    ): UtbetalingBeregningOutputDeltakelse {
        val aktiveUker = deltakelse.periode
            .subtractPeriods(stengtHosArrangor)
            .flatMap { it.splitByWeek() }
            .filter { it.getWeekdayCount() > 0 }

        // Den første perioden per påbegynte uke er den som får en tellende deltakelsesfaktor
        val beregnedePerioder = aktiveUker
            .groupBy { it.start.get(WeekFields.ISO.weekOfYear()) }
            .flatMap { (_, perioderISammeUke) ->
                perioderISammeUke.mapIndexed { index, periode ->
                    val satsPeriode = satser.first { sats -> periode.intersects(sats.periode) }
                    UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                        periode = periode,
                        faktor = if (index == 0) 1.0 else 0.0,
                        sats = satsPeriode.sats,
                    )
                }
            }

        // Kombiner sammenhengende perioder med samme sats
        val kombinertePerioder = beregnedePerioder
            .groupBy { it.sats }
            .flatMap { (_, perioderMedSammeSats) ->
                perioderMedSammeSats.fold(mutableListOf<MutableList<UtbetalingBeregningOutputDeltakelse.BeregnetPeriode>>()) { result, week ->
                    if (result.isEmpty() || result.last().last().periode.slutt != week.periode.start) {
                        result.add(mutableListOf(week))
                    } else {
                        result.last().add(week)
                    }
                    result
                }
            }
            .map { range ->
                UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(
                    periode = Periode.fromRange(range.map { it.periode }),
                    faktor = range.sumOf { it.faktor },
                    sats = range.first().sats,
                )
            }
            .toSet()

        return UtbetalingBeregningOutputDeltakelse(deltakelse.deltakelseId, kombinertePerioder)
    }

    fun calculateManedsverkBelop(periode: Periode, sats: ValutaBelop, antallPlasser: Int): ValutaBelop = calculateMonthsInPeriode(periode)
        .multiply(BigDecimal(sats.belop))
        .multiply(BigDecimal(antallPlasser))
        .setScale(0, RoundingMode.HALF_UP)
        .intValueExact()
        .withValuta(sats.valuta)

    fun calculateMonthsInPeriode(periode: Periode): BigDecimal {
        return getMonthsFraction(periode)
            .setScale(OUTPUT_PRECISION, RoundingMode.HALF_UP)
    }

    fun calculateWeeksInPeriode(periode: Periode): BigDecimal {
        return getWeeksFraction(periode)
            .setScale(OUTPUT_PRECISION, RoundingMode.HALF_UP)
    }

    fun calculateWholeWeeksInPeriode(periode: Periode): BigDecimal {
        val includedMonths = periode.splitByMonth().map { it.start.month }.toSet()
        return periode
            .splitByWeek()
            .count { week ->
                val monday = week.start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val wednesday = monday.with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY))
                val monthOfWeek = wednesday.month
                val weekdayCount = week.getWeekdayCount()

                if (includedMonths.contains(monthOfWeek)) {
                    weekdayCount > 0
                } else {
                    weekdayCount > 2
                }
            }
            .toBigDecimal()
            .setScale(OUTPUT_PRECISION, RoundingMode.HALF_UP)
    }

    fun calculateBelopForDeltakelser(
        valuta: Valuta,
        deltakelser: Set<UtbetalingBeregningOutputDeltakelse>,
    ): ValutaBelop {
        return deltakelser
            .flatMap { deltakelse ->
                deltakelse.perioder.map { BigDecimal(it.faktor).multiply(BigDecimal(it.sats.belop)) }
            }
            .sumOf { it }
            .setScale(0, RoundingMode.HALF_UP)
            .toInt()
            .withValuta(valuta)
    }

    fun calculateDeltakelseOutput(
        deltakelseId: UUID,
        perioder: List<Periode>,
        satser: Set<SatsPeriode>,
        calculateFaktor: (Periode) -> BigDecimal,
    ): UtbetalingBeregningOutputDeltakelse {
        val perioderOutput = perioder
            .asSequence()
            .flatMap { periode ->
                satser.mapNotNull { satsPeriode ->
                    periode.intersect(satsPeriode.periode)?.let { SatsPeriode(it, satsPeriode.sats) }
                }
            }
            .mapNotNull { (periode, sats) ->
                calculateFaktor(periode)
                    .setScale(OUTPUT_PRECISION, RoundingMode.HALF_UP)
                    .toDouble()
                    .takeIf { it > 0 }
                    ?.let { faktor ->
                        UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(periode, faktor, sats)
                    }
            }
            .toSet()
        return UtbetalingBeregningOutputDeltakelse(deltakelseId, perioderOutput)
    }

    private fun applyDeltakelsesprosent(
        tiltakskode: Tiltakskode,
        periode: Periode,
        fraction: BigDecimal,
        prosent: Double,
    ): BigDecimal {
        return if (
            tiltakskode == Tiltakskode.ARBEIDSFORBEREDENDE_TRENING &&
            periode.start >= DELTAKELSE_MONTHS_FRACTION_VERSION_3_DATE
        ) {
            applyDeltakelsesprosentV2(fraction, prosent)
        } else {
            applyDeltakelsesprosentV1(fraction, prosent)
        }
    }

    private fun applyDeltakelsesprosentV1(fraction: BigDecimal, prosent: Double): BigDecimal = if (prosent < 50) {
        fraction.divide(BigDecimal(2), CALCULATION_PRECISION, RoundingMode.HALF_UP)
    } else {
        fraction
    }

    private fun applyDeltakelsesprosentV2(fraction: BigDecimal, prosent: Double): BigDecimal = if (prosent <= 50) {
        fraction.divide(BigDecimal(2), CALCULATION_PRECISION, RoundingMode.HALF_UP)
    } else {
        fraction
    }

    private fun getMonthsFraction(periode: Periode): BigDecimal {
        return if (periode.getLastInclusiveDate().isBefore(DELTAKELSE_MONTHS_FRACTION_VERSION_2_DATE)) {
            getMonthsFractionV1(periode)
        } else {
            getMonthsFractionV2(periode)
        }
    }

    private fun getMonthsFractionV1(periode: Periode): BigDecimal {
        return periode
            .splitByMonth()
            .map { month ->
                val duration = month.getDurationInDays().toBigDecimal()
                val lengthOfMonth = month.start.lengthOfMonth().toBigDecimal()
                duration.divide(lengthOfMonth, CALCULATION_PRECISION, RoundingMode.HALF_UP)
            }
            .sumOf { it }
    }

    private fun getMonthsFractionV2(periode: Periode): BigDecimal {
        return periode
            .splitByMonth()
            .map { month ->
                val durationInMonth = month.getWeekdayCount().toBigDecimal()
                val durationOfMonth = Periode.forMonthOf(month.start).getWeekdayCount().toBigDecimal()
                durationInMonth.divide(durationOfMonth, CALCULATION_PRECISION, RoundingMode.HALF_UP)
            }
            .sumOf { it }
    }

    private fun getWeeksFraction(periode: Periode): BigDecimal {
        val weekdayCount = periode.getWeekdayCount()
        val weekdays = BigDecimal(5)
        return weekdayCount.toBigDecimal().divide(weekdays, CALCULATION_PRECISION, RoundingMode.HALF_UP)
    }
}
