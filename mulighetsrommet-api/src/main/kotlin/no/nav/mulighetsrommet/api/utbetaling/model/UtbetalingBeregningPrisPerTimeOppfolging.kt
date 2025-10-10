package no.nav.mulighetsrommet.api.utbetaling.model

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class UtbetalingBeregningPrisPerTimeOppfolging(
    override val input: Input,
    override val output: Output,
) : UtbetalingBeregning() {

    @Serializable
    data class Input(
        val belop: Int,
        val pris: Int,
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
