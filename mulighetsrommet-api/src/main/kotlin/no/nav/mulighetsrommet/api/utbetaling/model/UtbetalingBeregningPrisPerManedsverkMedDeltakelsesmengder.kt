package no.nav.mulighetsrommet.api.utbetaling.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Periode

@Serializable
data class UtbetalingBeregningPrisPerManedsverkMedDeltakelsesmengder(
    override val input: Input,
    override val output: Output,
) : UtbetalingBeregning() {

    @Serializable
    data class Input(
        val periode: Periode,
        val sats: Int,
        val stengt: Set<StengtPeriode>,
        val deltakelser: Set<DeltakelseDeltakelsesprosentPerioder>,
    ) : UtbetalingBeregningInput()

    @Serializable
    data class Output(
        override val belop: Int,
        override val deltakelser: Set<DeltakelseManedsverk>,
    ) : UtbetalingBeregningOutput()

    companion object {
        fun beregn(input: Input): UtbetalingBeregningPrisPerManedsverkMedDeltakelsesmengder {
            val stengtHosArrangor = input.stengt.map { it.periode }

            val manedsverk = input.deltakelser
                .map { deltakelse ->
                    UtbetalingBeregningHelpers.calculateManedsverkForDeltakelsesprosent(
                        deltakelse,
                        stengtHosArrangor,
                    )
                }
                .toSet()

            val belop = UtbetalingBeregningHelpers.caclulateBelopForDeltakelse(manedsverk, input.sats)

            return UtbetalingBeregningPrisPerManedsverkMedDeltakelsesmengder(input, Output(belop, manedsverk))
        }
    }
}
