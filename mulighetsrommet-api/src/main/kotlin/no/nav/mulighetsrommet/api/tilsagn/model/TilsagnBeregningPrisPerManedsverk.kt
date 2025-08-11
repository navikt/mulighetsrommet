package no.nav.mulighetsrommet.api.tilsagn.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningHelpers
import no.nav.mulighetsrommet.model.Periode
import java.math.BigDecimal
import java.math.RoundingMode

@Serializable
@SerialName("PRIS_PER_MANEDSVERK")
data class TilsagnBeregningPrisPerManedsverk(
    override val input: Input,
    override val output: Output,
) : TilsagnBeregning() {

    @Serializable
    @SerialName("PRIS_PER_MANEDSVERK")
    data class Input(
        val periode: Periode,
        val sats: Int,
        val antallPlasser: Int,
    ) : TilsagnBeregningInput()

    @Serializable
    @SerialName("PRIS_PER_MANEDSVERK")
    data class Output(
        override val belop: Int,
    ) : TilsagnBeregningOutput()

    companion object {
        fun beregn(input: Input): TilsagnBeregningPrisPerManedsverk {
            val (periode, sats, antallPlasser) = input

            val belop = UtbetalingBeregningHelpers.calculateManedsverk(periode)
                .multiply(BigDecimal(sats))
                .multiply(BigDecimal(antallPlasser))
                .setScale(0, RoundingMode.HALF_UP)
                .intValueExact()

            return TilsagnBeregningPrisPerManedsverk(input, Output(belop = belop))
        }
    }
}
