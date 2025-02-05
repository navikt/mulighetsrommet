package no.nav.mulighetsrommet.api.refusjon.model

data class RefusjonKravBeregningFri(
    override val input: Input,
    override val output: Output,
) : RefusjonKravBeregning() {

    data class Input(
        val belop: Int,
    ) : RefusjonKravBeregningInput()

    data class Output(
        override val belop: Int,
    ) : RefusjonKravBeregningOutput()

    companion object {
        fun beregn(input: Input): RefusjonKravBeregningFri {

            return RefusjonKravBeregningFri(
                input = input,
                output = Output(
                    belop = input.belop,
                ),
            )
        }
    }
}
