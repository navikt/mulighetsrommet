package no.nav.mulighetsrommet.api.utbetaling.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.*

@Serializable
data class UtbetalingBeregningFri(
    override val input: Input,
    override val output: Output,
) : UtbetalingBeregning() {

    @Serializable
    data class Input(
        val belop: Int,
        override val deltakelser: Set<DeltakelsePeriode>,
    ) : UtbetalingBeregningInput()

    @Serializable
    data class Output(
        override val belop: Int,
        override val deltakelser: Set<Deltakelse>,
    ) : UtbetalingBeregningOutput()

    companion object {
        fun beregn(input: Input): UtbetalingBeregningFri {
            return UtbetalingBeregningFri(
                input = input,
                output = Output(
                    belop = input.belop,
                    deltakelser = input.deltakelser.map {
                        Deltakelse(it.deltakelseId, 0.0)
                    }.toSet(),
                ),
            )
        }
    }

    @Serializable
    data class Deltakelse(
        @Serializable(with = UUIDSerializer::class)
        override val deltakelseId: UUID,
        override val faktor: Double,
    ) : UtbetalingBeregningOutputDeltakelse()
}
