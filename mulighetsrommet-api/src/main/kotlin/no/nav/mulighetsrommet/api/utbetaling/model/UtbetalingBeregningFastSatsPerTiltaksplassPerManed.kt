package no.nav.mulighetsrommet.api.utbetaling.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.ValutaBelop

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
        override val pris: ValutaBelop,
        val deltakelser: Set<UtbetalingBeregningOutputDeltakelse>,
    ) : UtbetalingBeregningOutput() {
        override fun deltakelser() = deltakelser
    }
}
