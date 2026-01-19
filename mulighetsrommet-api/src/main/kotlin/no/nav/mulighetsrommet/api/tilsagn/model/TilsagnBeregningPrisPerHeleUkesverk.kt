package no.nav.mulighetsrommet.api.tilsagn.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningHelpers
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.model.withValuta
import java.math.RoundingMode

@Serializable
@SerialName("PRIS_PER_HELE_UKESVERK")
data class TilsagnBeregningPrisPerHeleUkesverk(
    override val input: Input,
    override val output: Output,
) : TilsagnBeregning() {

    @Serializable
    @SerialName("PRIS_PER_UKESVERK")
    data class Input(
        val periode: Periode,
        val sats: ValutaBelop,
        val antallPlasser: Int,
        val prisbetingelser: String?,
    ) : TilsagnBeregningInput()

    @Serializable
    @SerialName("PRIS_PER_UKESVERK")
    data class Output(
        override val pris: ValutaBelop,
    ) : TilsagnBeregningOutput()

    companion object {
        fun beregn(input: Input): TilsagnBeregningPrisPerHeleUkesverk {
            val (periode, sats, antallPlasser) = input

            val belop = UtbetalingBeregningHelpers.calculateWholeWeeksInPeriode(periode)
                .multiply(sats.belop.toBigDecimal())
                .multiply(antallPlasser.toBigDecimal())
                .setScale(0, RoundingMode.HALF_UP)
                .intValueExact()
                .withValuta(sats.valuta)

            return TilsagnBeregningPrisPerHeleUkesverk(input, Output(belop))
        }
    }
}
