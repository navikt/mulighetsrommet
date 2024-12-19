package no.nav.mulighetsrommet.api.tilsagn.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("FRI")
data class TilsagnBeregningFri(
    override val input: Input,
    override val output: Output,
) : TilsagnBeregning() {

    @Serializable
    @SerialName("FRI")
    data class Input(
        val belop: Int,
    ) : TilsagnBeregningInput()

    @Serializable
    @SerialName("FRI")
    data class Output(
        override val belop: Int,
    ) : TilsagnBeregningOutput()
}
