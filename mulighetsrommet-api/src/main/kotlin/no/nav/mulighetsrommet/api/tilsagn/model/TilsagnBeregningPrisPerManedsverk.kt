package no.nav.mulighetsrommet.api.tilsagn.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningHelpers
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.ValutaBelop

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
        val sats: ValutaBelop,
        val antallPlasser: Int,
        val prisbetingelser: String?,
    ) : TilsagnBeregningInput()

    @Serializable
    @SerialName("PRIS_PER_MANEDSVERK")
    data class Output(
        override val pris: ValutaBelop,
    ) : TilsagnBeregningOutput()

    companion object {
        fun beregn(input: Input): TilsagnBeregningPrisPerManedsverk {
            val (periode, sats, antallPlasser) = input

            val belop = UtbetalingBeregningHelpers.calculateManedsverkBelop(periode, sats, antallPlasser)

            return TilsagnBeregningPrisPerManedsverk(input, Output(pris = belop))
        }
    }
}
