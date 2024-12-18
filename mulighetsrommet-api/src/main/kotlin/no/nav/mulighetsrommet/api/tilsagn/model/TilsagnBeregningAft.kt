package no.nav.mulighetsrommet.api.tilsagn.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonKravBeregningAft.Input
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonKravBeregningAft.Output
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import java.time.LocalDate

@Serializable
@SerialName("AFT")
data class TilsagnBeregningAft(
    override val input: Input,
    override val output: Output,
) : TilsagnBeregning() {

    @Serializable
    @SerialName("AFT")
    data class Input(
        @Serializable(with = LocalDateSerializer::class)
        val periodeStart: LocalDate,
        @Serializable(with = LocalDateSerializer::class)
        val periodeSlutt: LocalDate,
        val antallPlasser: Int,
    ) : TilsagnBeregningInput()

    @Serializable
    @SerialName("AFT")
    data class Output(
        val sats: Int,
        override val belop: Int,
    ) : TilsagnBeregningOutput()
}
