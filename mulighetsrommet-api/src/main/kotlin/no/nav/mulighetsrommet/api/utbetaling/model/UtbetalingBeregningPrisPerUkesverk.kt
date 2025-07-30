package no.nav.mulighetsrommet.api.utbetaling.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Periode

@Serializable
data class UtbetalingBeregningPrisPerUkesverk(
    override val input: Input,
    override val output: Output,
) : UtbetalingBeregning() {

    @Serializable
    data class Input(
        val periode: Periode,
        val sats: Int,
        val stengt: Set<StengtPeriode>,
        override val deltakelser: Set<DeltakelsePeriode>,
    ) : UtbetalingBeregningInput()

    @Serializable
    data class Output(
        override val belop: Int,
        override val deltakelser: Set<DeltakelseUkesverk>,
    ) : UtbetalingBeregningOutput()

    companion object {
        fun beregn(input: Input): UtbetalingBeregningPrisPerUkesverk {
            val stengtHosArrangor = input.stengt.map { it.periode }

            val ukesverk = input.deltakelser
                .map { deltakelse ->
                    UtbetalingBeregningHelpers.calculateUkesverk(deltakelse, stengtHosArrangor)
                }
                .toSet()

            val belop = UtbetalingBeregningHelpers.calculateBelopForDeltakelse(ukesverk, input.sats)

            return UtbetalingBeregningPrisPerUkesverk(input, Output(belop, ukesverk))
        }
    }
}
