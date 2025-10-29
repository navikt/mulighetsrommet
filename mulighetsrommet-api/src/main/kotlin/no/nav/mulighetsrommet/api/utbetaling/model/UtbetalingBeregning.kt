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

@Serializable
data class UtbetalingBeregningOutputDeltakelse(
    @Serializable(with = UUIDSerializer::class)
    val deltakelseId: UUID,
    val perioder: Set<BeregnetPeriode>,
) {
    @Serializable
    data class BeregnetPeriode(
        val periode: Periode,
        val faktor: Double,
        val sats: Int,
    )
}

@Serializable
data class SatsPeriode(
    val periode: Periode,
    val sats: Int,
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
     * Beregningen av månedsverk ble endret fra og med denne datoen
     */
    private val DELTAKELSE_MONTHS_FRACTION_VERSION_2_DATE: LocalDate = LocalDate.of(2025, 8, 1)

    fun calculateDeltakelseManedsverkForDeltakelsesprosent(
        deltakelse: DeltakelseDeltakelsesprosentPerioder,
        satser: Set<SatsPeriode>,
        stengtHosArrangor: List<Periode>,
    ): UtbetalingBeregningOutputDeltakelse {
        val perioder = deltakelse.perioder.flatMap { deltakelsePeriode ->
            deltakelsePeriode.periode
                .subtractPeriods(stengtHosArrangor)
                .map { DeltakelsesprosentPeriode(it, deltakelsePeriode.deltakelsesprosent) }
        }
        return calculateDeltakelseOutput(
            deltakelse.deltakelseId,
            perioder.map { it.periode },
            satser,
        ) { periode ->
            val prosent = perioder.find { it.periode == periode }?.deltakelsesprosent ?: 100.0
            getMonthsFraction(periode)
                .let { fraction ->
                    if (prosent < 50) {
                        fraction.divide(BigDecimal(2), CALCULATION_PRECISION, RoundingMode.HALF_UP)
                    } else {
                        fraction
                    }
                }
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
        val perioder = deltakelse.periode.subtractPeriods(stengtHosArrangor)
        return calculateDeltakelseOutput(deltakelse.deltakelseId, perioder, satser) { periode ->
            periode.splitByWeek().count { it.getWeekdayCount() > 0 }.toBigDecimal()
        }
    }

    fun calculateManedsverkBelop(periode: Periode, sats: Int, antallPlasser: Int): Int = calculateMonthsInPeriode(periode)
        .multiply(BigDecimal(sats))
        .multiply(BigDecimal(antallPlasser))
        .setScale(0, RoundingMode.HALF_UP)
        .intValueExact()

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
        deltakelser: Set<UtbetalingBeregningOutputDeltakelse>,
        satser: Set<SatsPeriode>,
    ): Int {
        // TODO: ta høyde for flere satser
        val sats = satser.first().sats
        return deltakelser
            .flatMap { deltakelse -> deltakelse.perioder.map { it.faktor } }
            .fold(BigDecimal.ZERO) { sum, faktor ->
                sum.add(BigDecimal(faktor))
            }
            .multiply(BigDecimal(sats))
            .setScale(0, RoundingMode.HALF_UP)
            .toInt()
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
