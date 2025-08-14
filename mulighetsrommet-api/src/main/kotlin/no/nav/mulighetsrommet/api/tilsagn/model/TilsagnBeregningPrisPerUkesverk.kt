package no.nav.mulighetsrommet.api.tilsagn.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningHelpers
import no.nav.mulighetsrommet.model.Periode
import java.math.RoundingMode

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
        val prisbetingelser: String?,
    ) : TilsagnBeregningInput()

    @Serializable
    @SerialName("PRIS_PER_UKESVERK")
    data class Output(
        override val belop: Int,
    ) : TilsagnBeregningOutput()

    companion object {
        fun beregn(input: Input): TilsagnBeregningPrisPerUkesverk {
            val (periode, sats, antallPlasser) = input

            val belop = UtbetalingBeregningHelpers.calculateUkesverk(periode)
                .multiply(sats.toBigDecimal())
                .multiply(antallPlasser.toBigDecimal())
                .setScale(0, RoundingMode.HALF_UP)
                .toInt()

            return TilsagnBeregningPrisPerUkesverk(input, Output(belop))
        }
    }
}
