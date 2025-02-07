package no.nav.mulighetsrommet.api.utbetaling.model

data class UtbetalingBeregningFri(
    override val input: Input,
    override val output: Output,
) : UtbetalingBeregning() {

    data class Input(
        val belop: Int,
    ) : UtbetalingBeregningInput()

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
