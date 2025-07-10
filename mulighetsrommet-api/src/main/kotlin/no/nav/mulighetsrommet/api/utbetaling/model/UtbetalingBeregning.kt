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

    @OptIn(ExperimentalStdlibApi::class)
    fun getDigest(): String {
        return (input.hashCode() + output.hashCode()).toHexString()
    }
}

sealed class UtbetalingBeregningInput

sealed class UtbetalingBeregningOutput {
    abstract val belop: Int
}

@Serializable
data class StengtPeriode(
    val periode: Periode,
    val beskrivelse: String,
)

@Serializable
data class DeltakelseDeltakelsesprosentPerioder(
    @Serializable(with = UUIDSerializer::class)
    val deltakelseId: UUID,
    val perioder: List<DeltakelsesprosentPeriode>,
)

@Serializable
data class DeltakelsesprosentPeriode(
    val periode: Periode,
    val deltakelsesprosent: Double,
)

interface DeltakelseFaktor {
    fun getFaktor(): Double
}

@Serializable
data class DeltakelseManedsverk(
    @Serializable(with = UUIDSerializer::class)
    val deltakelseId: UUID,
    val manedsverk: Double,
) : DeltakelseFaktor {
    override fun getFaktor(): Double = manedsverk
}

@Serializable
data class DeltakelsePeriode(
    @Serializable(with = UUIDSerializer::class)
    val deltakelseId: UUID,
    val periode: Periode,
)

@Serializable
data class DeltakelseUkesverk(
    @Serializable(with = UUIDSerializer::class)
    val deltakelseId: UUID,
    val ukesverk: Double,
) : DeltakelseFaktor {
    override fun getFaktor(): Double = ukesverk
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

    fun calculateManedsverkForDeltakelsesprosent(
        deltakelse: DeltakelseDeltakelsesprosentPerioder,
        stengtHosArrangor: List<Periode>,
        periode: Periode,
    ): DeltakelseManedsverk {
        val totalDuration = periode.getDurationInDays().toBigDecimal()

        val manedsverk = deltakelse.perioder
            .flatMap { deltakelsePeriode ->
                deltakelsePeriode.periode
                    .subtractPeriods(stengtHosArrangor)
                    .map { DeltakelsesprosentPeriode(it, deltakelsePeriode.deltakelsesprosent) }
            }
            .map { deltakelsePeriode ->
                val deltakelsesprosent = if (deltakelsePeriode.deltakelsesprosent < 50) {
                    BigDecimal(50)
                } else {
                    BigDecimal(100)
                }
                calculateManedsverkFraction(deltakelsePeriode.periode, totalDuration).multiply(deltakelsesprosent)
            }
            .sumOf { it }
            .setScale(OUTPUT_PRECISION, RoundingMode.HALF_UP)
            .toDouble()

        return DeltakelseManedsverk(deltakelse.deltakelseId, manedsverk)
    }

    fun calculateManedsverkFraction(
        periode: Periode,
        totalDuration: BigDecimal,
    ): BigDecimal {
        val overlapDuration = periode.getDurationInDays().toBigDecimal()
        val overlapFraction = overlapDuration.divide(totalDuration, CALCULATION_PRECISION, RoundingMode.HALF_UP)
        return overlapFraction.divide(BigDecimal(100), CALCULATION_PRECISION, RoundingMode.HALF_UP)
    }

    fun calculateUkesverk(
        deltakelse: DeltakelsePeriode,
        stengtHosArrangor: List<Periode>,
    ): DeltakelseUkesverk {
        val ukesverk = deltakelse.periode
            .subtractPeriods(stengtHosArrangor)
            .map { periode ->
                calculateUkesverkFraction(periode)
            }
            .sumOf { it }
            .setScale(OUTPUT_PRECISION, RoundingMode.HALF_UP)
            .toDouble()
        return DeltakelseUkesverk(deltakelse.deltakelseId, ukesverk)
    }

    fun calculateUkesverkFraction(periode: Periode): BigDecimal {
        val days = periode.getDurationInDays().toBigDecimal()
        return days.divide(BigDecimal(7), CALCULATION_PRECISION, RoundingMode.HALF_UP)
    }

    fun caclulateBelopForDeltakelse(deltakelser: Set<DeltakelseFaktor>, sats: Int): Int {
        return deltakelser
            .fold(BigDecimal.ZERO) { sum, deltakelse ->
                sum.add(BigDecimal(deltakelse.getFaktor()))
            }
            .multiply(BigDecimal(sats))
            .setScale(0, RoundingMode.HALF_UP)
            .toInt()
    }
}
