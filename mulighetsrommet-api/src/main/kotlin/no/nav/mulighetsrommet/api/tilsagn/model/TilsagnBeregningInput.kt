package no.nav.mulighetsrommet.api.tilsagn.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import java.time.LocalDate

@Serializable
sealed class TilsagnBeregningInput {
    @Serializable
    @SerialName("AFT")
    data class AFT(
        @Serializable(with = LocalDateSerializer::class)
        val periodeStart: LocalDate,
        @Serializable(with = LocalDateSerializer::class)
        val periodeSlutt: LocalDate,
        val antallPlasser: Int,
        val sats: Int,
    ) : TilsagnBeregningInput()

    @Serializable
    @SerialName("FRI")
    data class Fri(val belop: Int) : TilsagnBeregningInput()
}
