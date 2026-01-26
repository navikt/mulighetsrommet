package no.nav.mulighetsrommet.api.utbetaling.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.ValutaBelop

@Serializable
data class UtbetalingBeregningFri(
    override val input: Input,
    override val output: Output,
) : UtbetalingBeregning() {

    @Serializable
    data class Input(val pris: ValutaBelop) : UtbetalingBeregningInput() {
        override fun deltakelser() = emptySet<UtbetalingBeregningInputDeltakelse>()
    }

    @Serializable
    data class Output(override val pris: ValutaBelop) : UtbetalingBeregningOutput() {
        override fun deltakelser() = emptySet<UtbetalingBeregningOutputDeltakelse>()
    }

    companion object {
        fun beregn(input: Input): UtbetalingBeregningFri {
            return UtbetalingBeregningFri(
                input = input,
                output = Output(pris = input.pris),
            )
        }
    }
}
