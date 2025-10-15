package no.nav.mulighetsrommet.api.utbetaling.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Periode

@Serializable
data class UtbetalingBeregningPrisPerTimeOppfolging(
    override val input: Input,
    override val output: Output,
) : UtbetalingBeregning() {

    @Serializable
    data class Input(
        val periode: Periode,
        val belop: Int,
        val sats: Int,
        val stengt: Set<StengtPeriode>,
        val deltakelser: Set<DeltakelsePeriode>,
    ) : UtbetalingBeregningInput() {
        override fun deltakelser() = deltakelser
    }

    @Serializable
    data class Output(override val belop: Int) : UtbetalingBeregningOutput() {
        override fun deltakelser() = emptySet<UtbetalingBeregningOutputDeltakelse>()
    }

    companion object {
        fun beregn(input: Input): UtbetalingBeregningPrisPerTimeOppfolging {
            return UtbetalingBeregningPrisPerTimeOppfolging(
                input = input,
                output = Output(belop = input.belop),
            )
        }
    }
}
