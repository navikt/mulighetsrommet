package no.nav.mulighetsrommet.api.utbetaling.model

import kotlinx.serialization.Serializable

@Serializable
data class UtbetalingBeregningFri(
    override val input: Input,
    override val output: Output,
) : UtbetalingBeregning() {

    @Serializable
    data class Input(
        val belop: Int,
    ) : UtbetalingBeregningInput()

    @Serializable
    data class Output(
        override val belop: Int,
    ) : UtbetalingBeregningOutput()

    companion object {
        fun beregn(input: Input): UtbetalingBeregningFri {
            return UtbetalingBeregningFri(
                input = input,
                output = Output(
                    belop = input.belop,
                ),
            )
        }
    }
}
