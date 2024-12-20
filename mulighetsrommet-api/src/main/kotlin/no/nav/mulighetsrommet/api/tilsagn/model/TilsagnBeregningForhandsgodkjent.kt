package no.nav.mulighetsrommet.api.tilsagn.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import java.time.LocalDate

@Serializable
@SerialName("FORHANDSGODKJENT")
data class TilsagnBeregningForhandsgodkjent(
    override val input: Input,
    override val output: Output,
) : TilsagnBeregning() {

    @Serializable
    @SerialName("FORHANDSGODKJENT")
    data class Input(
        @Serializable(with = LocalDateSerializer::class)
        val periodeStart: LocalDate,
        @Serializable(with = LocalDateSerializer::class)
        val periodeSlutt: LocalDate,
        val sats: Int,
        val antallPlasser: Int,
    ) : TilsagnBeregningInput()

    @Serializable
    @SerialName("FORHANDSGODKJENT")
    data class Output(
        override val belop: Int,
    ) : TilsagnBeregningOutput()
}
