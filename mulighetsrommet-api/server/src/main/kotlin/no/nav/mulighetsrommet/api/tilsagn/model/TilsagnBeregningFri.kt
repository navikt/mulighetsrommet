package no.nav.mulighetsrommet.api.tilsagn.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.ValutaBelop

@Serializable
@SerialName("FRI")
data class TilsagnBeregningFri(
    override val input: Input,
    override val output: Output,
) : TilsagnBeregning() {

    @Serializable
    @SerialName("FRI")
    data class Input(
        val pris: ValutaBelop,
    ) : TilsagnBeregningInput()

    @Serializable
    @SerialName("FRI")
    data class Output(
        override val pris: ValutaBelop,
    ) : TilsagnBeregningOutput()

    companion object {
        fun beregn(input: Input): TilsagnBeregningFri {
            return TilsagnBeregningFri(input, Output(input.pris))
        }
    }
}
