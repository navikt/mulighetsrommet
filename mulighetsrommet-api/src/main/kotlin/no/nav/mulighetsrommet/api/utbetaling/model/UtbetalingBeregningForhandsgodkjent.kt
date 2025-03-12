package no.nav.mulighetsrommet.api.utbetaling.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

/**
 * Presisjon underveis for å oppnå en god beregning av totalbeløpet.
 */
private const val CALCULATION_PRECISION = 20

/**
 * Presisjon for den delen av beregningen som blir med i output og tilgjengelig for innsyn, test, etc.
 */
private const val OUTPUT_PRECISION = 5

@Serializable
data class UtbetalingBeregningForhandsgodkjent(
    override val input: Input,
    override val output: Output,
) : UtbetalingBeregning() {

    @Serializable
    data class Input(
        val periode: Periode,
        val sats: Int,
        val stengt: Set<StengtPeriode>,
        val deltakelser: Set<DeltakelsePerioder>,
    ) : UtbetalingBeregningInput()

    @Serializable
    data class Output(
        override val belop: Int,
        val deltakelser: Set<DeltakelseManedsverk>,
    ) : UtbetalingBeregningOutput()

    companion object {
        fun beregn(input: Input): UtbetalingBeregningForhandsgodkjent {
            val totalDuration = input.periode.getDurationInDays().toBigDecimal()

            val manedsverk = input.deltakelser
                .map { deltakelse ->
                    calculateManedsverk(deltakelse, input.stengt.map { it.periode }, totalDuration)
                }
                .toSet()

            val belop = manedsverk
                .fold(BigDecimal.ZERO) { sum, deltakelse ->
                    sum.add(BigDecimal(deltakelse.manedsverk))
                }
                .multiply(BigDecimal(input.sats))
                .setScale(CALCULATION_PRECISION, RoundingMode.HALF_UP)
                .toInt()

            val output = Output(
                belop = belop,
                deltakelser = manedsverk,
            )

            return UtbetalingBeregningForhandsgodkjent(input, output)
        }

        private fun calculateManedsverk(
            deltakelse: DeltakelsePerioder,
            stengtHosArrangor: List<Periode>,
            totalDuration: BigDecimal,
        ): DeltakelseManedsverk {
            val manedsverk = deltakelse.perioder
                .flatMap { deltakelsePeriode ->
                    deltakelsePeriode.periode
                        .subtractPeriods(stengtHosArrangor)
                        .map { DeltakelsePeriode(periode = it, deltakelsePeriode.deltakelsesprosent) }
                }
                .map { deltakelsePeriode ->
                    calculateManedsverkFraction(deltakelsePeriode, totalDuration)
                }
                .sumOf { it }
                .setScale(OUTPUT_PRECISION, RoundingMode.HALF_UP)
                .toDouble()

            return DeltakelseManedsverk(deltakelse.deltakelseId, manedsverk)
        }

        private fun calculateManedsverkFraction(
            deltakelsePeriode: DeltakelsePeriode,
            totalDuration: BigDecimal,
        ): BigDecimal {
            val overlapDuration = deltakelsePeriode.periode
                .getDurationInDays()
                .toBigDecimal()

            val overlapFraction = overlapDuration.divide(totalDuration, CALCULATION_PRECISION, RoundingMode.HALF_UP)

            val deltakelsesprosent = if (deltakelsePeriode.deltakelsesprosent < 50) {
                BigDecimal(50)
            } else {
                BigDecimal(100)
            }

            return overlapFraction
                .multiply(deltakelsesprosent)
                .divide(BigDecimal(100), CALCULATION_PRECISION, RoundingMode.HALF_UP)
        }
    }
}

@Serializable
data class StengtPeriode(
    val periode: Periode,
    val beskrivelse: String,
)

@Serializable
data class DeltakelsePerioder(
    @Serializable(with = UUIDSerializer::class)
    val deltakelseId: UUID,
    val perioder: List<DeltakelsePeriode>,
)

@Serializable
data class DeltakelsePeriode(
    val periode: Periode,
    val deltakelsesprosent: Double,
)

@Serializable
data class DeltakelseManedsverk(
    @Serializable(with = UUIDSerializer::class)
    val deltakelseId: UUID,
    val manedsverk: Double,
)
