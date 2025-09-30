package no.nav.mulighetsrommet.api.utbetaling.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.*

@Serializable
sealed class UtbetalingBeregning {
    abstract val input: UtbetalingBeregningInput
    abstract val output: UtbetalingBeregningOutput

    fun getDigest(): String {
        return (input.hashCode() + output.hashCode()).toHexString()
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
    abstract val belop: Int
    abstract fun deltakelser(): Set<UtbetalingBeregningOutputDeltakelse>
}

sealed class UtbetalingBeregningOutputDeltakelse {
    abstract val deltakelseId: UUID
    abstract val faktor: Double
}

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
data class DeltakelseManedsverk(
    @Serializable(with = UUIDSerializer::class)
    override val deltakelseId: UUID,
    val manedsverk: Double,
) : UtbetalingBeregningOutputDeltakelse() {
    override val faktor: Double
        get() = manedsverk
}

@Serializable
data class DeltakelsePeriode(
    @Serializable(with = UUIDSerializer::class)
    override val deltakelseId: UUID,
    val periode: Periode,
) : UtbetalingBeregningInputDeltakelse() {
    override fun periode() = periode
}

@Serializable
data class DeltakelseUkesverk(
    @Serializable(with = UUIDSerializer::class)
    override val deltakelseId: UUID,
    val ukesverk: Double,
) : UtbetalingBeregningOutputDeltakelse() {
    override val faktor: Double
        get() = ukesverk
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
     * Beregningen av månedsverk ble endret fra og med denne datoen
     */
    private val DELTAKELSE_MONTHS_FRACTION_VERSION_2_DATE: LocalDate = LocalDate.of(2025, 8, 1)

    fun calculateDeltakelseManedsverkForDeltakelsesprosent(
        deltakelse: DeltakelseDeltakelsesprosentPerioder,
        stengtHosArrangor: List<Periode>,
    ): DeltakelseManedsverk {
        val manedsverk = deltakelse.perioder
            .flatMap { deltakelsePeriode ->
                deltakelsePeriode.periode
                    .subtractPeriods(stengtHosArrangor)
                    .map { DeltakelsesprosentPeriode(it, deltakelsePeriode.deltakelsesprosent) }
            }
            .map { deltakelsePeriode ->
                val fraction = getMonthsFraction(deltakelsePeriode.periode)
                if (deltakelsePeriode.deltakelsesprosent < 50) {
                    fraction.divide(BigDecimal(2), CALCULATION_PRECISION, RoundingMode.HALF_UP)
                } else {
                    fraction
                }
            }
            .sumOf { it }
            .setScale(OUTPUT_PRECISION, RoundingMode.HALF_UP)
            .toDouble()

        return DeltakelseManedsverk(deltakelse.deltakelseId, manedsverk)
    }

    fun calculateDeltakelseManedsverk(
        deltakelse: DeltakelsePeriode,
        stengtHosArrangor: List<Periode>,
    ): DeltakelseManedsverk {
        val manedsverk = deltakelse.periode
            .subtractPeriods(stengtHosArrangor)
            .map { deltakelsePeriode ->
                getMonthsFraction(deltakelsePeriode)
            }
            .sumOf { it }
            .setScale(OUTPUT_PRECISION, RoundingMode.HALF_UP)
            .toDouble()
        return DeltakelseManedsverk(deltakelse.deltakelseId, manedsverk)
    }

    fun calculateManedsverkBelop(periode: Periode, sats: Int, antallPlasser: Int): Int = calculateMonthsInPeriode(periode)
        .multiply(BigDecimal(sats))
        .multiply(BigDecimal(antallPlasser))
        .setScale(0, RoundingMode.HALF_UP)
        .intValueExact()

    fun calculateDeltakelseUkesverk(
        deltakelse: DeltakelsePeriode,
        stengtHosArrangor: List<Periode>,
    ): DeltakelseUkesverk {
        val ukesverk = deltakelse.periode
            .subtractPeriods(stengtHosArrangor)
            .map { periode ->
                getWeeksFraction(periode)
            }
            .sumOf { it }
            .setScale(OUTPUT_PRECISION, RoundingMode.HALF_UP)
            .toDouble()
        return DeltakelseUkesverk(deltakelse.deltakelseId, ukesverk)
    }

    fun calculateDeltakelseHeleUkesverk(
        deltakelse: DeltakelsePeriode,
        stengtHosArrangor: List<Periode>,
    ): DeltakelseUkesverk {
        val ukesverk = deltakelse.periode
            .subtractPeriods(stengtHosArrangor)
            .map { periode ->
                periode
                    .splitByWeek()
                    .count { week ->
                        week.getWeekdayCount() > 0
                    }
                    .toBigDecimal()
            }
            .sumOf { it }
            .setScale(OUTPUT_PRECISION, RoundingMode.HALF_UP)
            .toDouble()
        return DeltakelseUkesverk(deltakelse.deltakelseId, ukesverk)
    }

    fun calculateMonthsInPeriode(periode: Periode): BigDecimal {
        return getMonthsFraction(periode)
            .setScale(OUTPUT_PRECISION, RoundingMode.HALF_UP)
    }

    fun calculateWeeksInPeriode(periode: Periode): BigDecimal {
        return getWeeksFraction(periode)
            .setScale(OUTPUT_PRECISION, RoundingMode.HALF_UP)
    }

    fun calculateWholeWeeksInPeriode(periode: Periode): BigDecimal {
        return periode
            .splitByWeek()
            .count { week ->
                val monday = week.start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val sunday = week.getLastInclusiveDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                val weekdayCount = week.getWeekdayCount()
                weekdayCount >= 3 || (monday.month == sunday.month && weekdayCount > 0)
            }
            .toBigDecimal()
            .setScale(OUTPUT_PRECISION, RoundingMode.HALF_UP)
    }

    fun calculateBelopForDeltakelse(deltakelser: Set<UtbetalingBeregningOutputDeltakelse>, sats: Int): Int {
        return deltakelser
            .fold(BigDecimal.ZERO) { sum, deltakelse ->
                sum.add(BigDecimal(deltakelse.faktor))
            }
            .multiply(BigDecimal(sats))
            .setScale(0, RoundingMode.HALF_UP)
            .toInt()
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
