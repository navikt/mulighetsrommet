package no.nav.mulighetsrommet.api.utbetaling.model

import kotlinx.serialization.Serializable

@Serializable
data class UtbetalingBeregningPrisPerManedsverk(
    override val input: Input,
    override val output: Output,
) : UtbetalingBeregning() {

    @Serializable
    data class Input(
        val satser: Set<SatsPeriode>,
        val stengt: Set<StengtPeriode>,
        val deltakelser: Set<DeltakelsePeriode>,
    ) : UtbetalingBeregningInput() {
        override fun deltakelser() = deltakelser
    }

    @Serializable
    data class Output(
        override val belop: Int,
        val deltakelser: Set<UtbetalingBeregningOutputDeltakelse>,
    ) : UtbetalingBeregningOutput() {
        override fun deltakelser() = deltakelser
    }

    companion object {
        fun beregn(input: Input): UtbetalingBeregningPrisPerManedsverk {
            val stengtHosArrangor = input.stengt.map { it.periode }

            val manedsverk = input.deltakelser
                .map { deltakelse ->
                    UtbetalingBeregningHelpers.calculateDeltakelseManedsverk(
                        deltakelse,
                        input.satser,
                        stengtHosArrangor,
                    )
                }
                .toSet()

            val belop = UtbetalingBeregningHelpers.calculateBelopForDeltakelser(manedsverk, input.satser)

            return UtbetalingBeregningPrisPerManedsverk(input, Output(belop, manedsverk))
        }
    }
}
