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
                    // TODO: Tom per nå, men vi tenker å lagre deltakelser på fri modell også
                    deltakelser = emptySet(),
                ),
            )
        }
    }

    @Serializable
    data class Deltakelse(
        @Serializable(with = UUIDSerializer::class)
        override val deltakelseId: UUID,
    ) : UtbetalingBeregningDeltakelse()
}
