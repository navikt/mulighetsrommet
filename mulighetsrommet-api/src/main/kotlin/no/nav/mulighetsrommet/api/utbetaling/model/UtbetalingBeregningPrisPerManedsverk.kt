package no.nav.mulighetsrommet.api.utbetaling.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Periode

@Serializable
data class UtbetalingBeregningPrisPerManedsverk(
    override val input: Input,
    override val output: Output,
) : UtbetalingBeregning() {

    @Serializable
    data class Input(
        val periode: Periode,
        val sats: Int,
        val stengt: Set<StengtPeriode>,
        val deltakelser: Set<DeltakelsePeriode>,
    ) : UtbetalingBeregningInput() {
        override fun deltakelser() = deltakelser
    }

    @Serializable
    data class Output(
        override val belop: Int,
        val deltakelser: Set<DeltakelseManedsverk>,
    ) : UtbetalingBeregningOutput() {
        override fun deltakelser() = deltakelser
    }

    companion object {
        fun beregn(input: Input): UtbetalingBeregningPrisPerManedsverk {
            val stengtHosArrangor = input.stengt.map { it.periode }

            val manedsverk = input.deltakelser
                .map { deltakelse ->
                    UtbetalingBeregningHelpers.calculateDeltakelseManedsverk(deltakelse, stengtHosArrangor)
                }
                .flatten()
                .toSet()

            val belop = UtbetalingBeregningHelpers.calculateBelopForDeltakelse(manedsverk, input.sats)

            return UtbetalingBeregningPrisPerManedsverk(input, Output(belop, manedsverk))
        }
    }
}
