package no.nav.mulighetsrommet.api.utbetaling.model

import kotlinx.serialization.Serializable

@Serializable
data class UtbetalingBeregningFastSatsPerTiltaksplassPerManed(
    override val input: Input,
    override val output: Output,
) : UtbetalingBeregning() {

    @Serializable
    data class Input(
        val satser: Set<SatsPeriode>,
        val stengt: Set<StengtPeriode>,
        val deltakelser: Set<DeltakelseDeltakelsesprosentPerioder>,
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
        fun beregn(input: Input): UtbetalingBeregningFastSatsPerTiltaksplassPerManed {
            val stengtHosArrangor = input.stengt.map { it.periode }

            val manedsverk = input.deltakelser
                .map { deltakelse ->
                    UtbetalingBeregningHelpers.calculateDeltakelseManedsverkForDeltakelsesprosent(
                        deltakelse,
                        input.satser,
                        stengtHosArrangor,
                    )
                }
                .toSet()

            val belop = UtbetalingBeregningHelpers.calculateBelopForDeltakelser(manedsverk)

            return UtbetalingBeregningFastSatsPerTiltaksplassPerManed(input, Output(belop, manedsverk))
        }
    }
}
