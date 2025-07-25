package no.nav.mulighetsrommet.api.tilsagn.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Periode
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Presisjon underveis for å oppnå en god beregning av totalbeløpet.
 */
private const val CALCULATION_PRECISION = 20

@Serializable
@SerialName("PRIS_PER_UKESVERK")
data class TilsagnBeregningPrisPerUkesverk(
    override val input: Input,
    override val output: Output,
) : TilsagnBeregning() {

    @Serializable
    @SerialName("PRIS_PER_UKESVERK")
    data class Input(
        val periode: Periode,
        val sats: Int,
        val antallPlasser: Int,
    ) : TilsagnBeregningInput()

    @Serializable
    @SerialName("PRIS_PER_UKESVERK")
    data class Output(
        override val belop: Int,
    ) : TilsagnBeregningOutput()

    companion object {
        fun beregn(input: Input): TilsagnBeregningPrisPerUkesverk {
            val (periode, sats, antallPlasser) = input

            val belop = ukesverk(periode)
                .multiply(sats.toBigDecimal())
                .multiply(antallPlasser.toBigDecimal())
                .setScale(0, RoundingMode.HALF_UP)
                .toInt()

            return TilsagnBeregningPrisPerUkesverk(input, Output(belop))
        }

        fun ukesverk(periode: Periode): BigDecimal = periode
            .getDurationInDays()
            .toBigDecimal()
            .divide(7.toBigDecimal(), CALCULATION_PRECISION, RoundingMode.HALF_UP)
    }
}
