package no.nav.mulighetsrommet.api.utbetaling.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.math.BigDecimal
import java.math.RoundingMode
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
    private const val CALCULATION_PRECISION = 20

    /**
     * Presisjon for den delen av beregningen som blir med i output og tilgjengelig for innsyn, test, etc.
     */
    private const val OUTPUT_PRECISION = 5

    fun calculateManedsverkForDeltakelsesprosent(
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
                val fraction = calculateManedsverk(deltakelsePeriode.periode)
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

    fun calculateManedsverk(
        deltakelse: DeltakelsePeriode,
        stengtHosArrangor: List<Periode>,
    ): DeltakelseManedsverk {
        val manedsverk = deltakelse.periode
            .subtractPeriods(stengtHosArrangor)
            .map { deltakelsePeriode ->
                calculateManedsverk(deltakelsePeriode)
            }
            .sumOf { it }
            .setScale(OUTPUT_PRECISION, RoundingMode.HALF_UP)
            .toDouble()

        return DeltakelseManedsverk(deltakelse.deltakelseId, manedsverk)
    }

    private fun calculateManedsverk(periode: Periode): BigDecimal {
        return periode
            .splitByMonth()
            .map {
                val duration = it.getDurationInDays().toBigDecimal()
                duration.divide(it.start.lengthOfMonth().toBigDecimal(), CALCULATION_PRECISION, RoundingMode.HALF_UP)
            }
            .sumOf { it }
    }

    fun calculateUkesverk(
        deltakelse: DeltakelsePeriode,
        stengtHosArrangor: List<Periode>,
    ): DeltakelseUkesverk {
        val ukesverk = deltakelse.periode
            .subtractPeriods(stengtHosArrangor)
            .map { periode ->
                calculateUkesverk(periode)
            }
            .sumOf { it }
            .setScale(OUTPUT_PRECISION, RoundingMode.HALF_UP)
            .toDouble()
        return DeltakelseUkesverk(deltakelse.deltakelseId, ukesverk)
    }

    private fun calculateUkesverk(periode: Periode): BigDecimal {
        val days = periode.getDurationInDays().toBigDecimal()
        return days.divide(BigDecimal(7), CALCULATION_PRECISION, RoundingMode.HALF_UP)
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
}
