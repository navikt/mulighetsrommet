package no.nav.mulighetsrommet.api.utbetaling.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.ValutaBelop

@Serializable
data class UtbetalingBeregningPrisPerTimeOppfolging(
    override val input: Input,
    override val output: Output,
) : UtbetalingBeregning() {

    override fun deltakelsePerioder(): Set<DeltakelsePeriode> {
        return input.deltakelser().map {
            DeltakelsePeriode(it.deltakelseId, it.periode())
        }.toSet()
    }

    @Serializable
    data class Input(
        val satser: Set<SatsPeriode>,
        val pris: ValutaBelop,
        val stengt: Set<StengtPeriode>,
        val deltakelser: Set<DeltakelsePeriode>,
    ) : UtbetalingBeregningInput() {
        override fun deltakelser() = deltakelser
    }

    @Serializable
    data class Output(override val pris: ValutaBelop) : UtbetalingBeregningOutput() {
        override fun deltakelser() = emptySet<UtbetalingBeregningOutputDeltakelse>()
    }

    companion object {
        fun beregn(input: Input): UtbetalingBeregningPrisPerTimeOppfolging {
            return UtbetalingBeregningPrisPerTimeOppfolging(
                input = input,
                output = Output(pris = input.pris),
            )
        }
    }
}
