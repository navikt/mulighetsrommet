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
data class UtbetalingBeregningPrisPerUkesverk(
    override val input: Input,
    override val output: Output,
) : UtbetalingBeregning() {

    @Serializable
    data class Input(
        val periode: Periode,
        val sats: Int,
        val stengt: Set<StengtPeriode>,
        val deltakelser: Set<DeltakelsePeriode>,
    ) : UtbetalingBeregningInput()

    @Serializable
    data class Output(
        override val belop: Int,
        val deltakelser: Set<DeltakelseUkesverk>,
    ) : UtbetalingBeregningOutput()

    companion object {
        fun beregn(input: Input): UtbetalingBeregningPrisPerUkesverk {
            val ukesverk = input.deltakelser
                .map { deltakelse ->
                    calculateUkesverk(deltakelse, input.stengt.map { it.periode })
                }
                .toSet()

            val belop = ukesverk
                .fold(BigDecimal.ZERO) { sum, deltakelse ->
                    sum.add(BigDecimal(deltakelse.ukesverk))
                }
                .multiply(BigDecimal(input.sats))
                .setScale(0, RoundingMode.HALF_UP)
                .toInt()

            val output = Output(
                belop = belop,
                deltakelser = ukesverk,
            )

            return UtbetalingBeregningPrisPerUkesverk(input, output)
        }

        private fun calculateUkesverk(
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

        private fun calculateUkesverkFraction(periode: Periode): BigDecimal {
            val days = periode.getDurationInDays().toBigDecimal()
            return days.divide(BigDecimal(7), CALCULATION_PRECISION, RoundingMode.HALF_UP)
        }
    }
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
)
