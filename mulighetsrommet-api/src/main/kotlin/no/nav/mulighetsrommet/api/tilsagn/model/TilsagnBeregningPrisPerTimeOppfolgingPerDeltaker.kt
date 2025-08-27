package no.nav.mulighetsrommet.api.tilsagn.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningHelpers
import no.nav.mulighetsrommet.model.Periode
import java.math.BigDecimal
import java.math.RoundingMode

@Serializable
@SerialName("PRIS_PER_TIME_OPPFOLGING")
data class TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker(
    override val input: Input,
    override val output: Output,
) : TilsagnBeregning() {

    @Serializable
    @SerialName("PRIS_PER_TIME_OPPFOLGING")
    data class Input(
        val periode: Periode,
        val sats: Int,
        val antallPlasser: Int,
        val antallTimerOppfolgingPerDeltaker: Int,
        val prisbetingelser: String?,
    ) : TilsagnBeregningInput()

    @Serializable
    @SerialName("PRIS_PER_TIME_OPPFOLGING")
    data class Output(
        override val belop: Int,
    ) : TilsagnBeregningOutput()

    companion object {
        fun beregn(input: Input): TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker {
            val belop = UtbetalingBeregningHelpers.calculateManedsverk(input.periode)
                .multiply(BigDecimal(input.sats))
                .multiply(BigDecimal(input.antallPlasser))
                .multiply(BigDecimal(input.antallTimerOppfolgingPerDeltaker))
                .setScale(0, RoundingMode.HALF_UP)
                .intValueExact()

            return TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker(input, Output(belop = belop))
        }
    }
}
