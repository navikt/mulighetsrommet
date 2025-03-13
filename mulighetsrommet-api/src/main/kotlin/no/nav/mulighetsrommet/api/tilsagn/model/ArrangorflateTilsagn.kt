package no.nav.mulighetsrommet.api.tilsagn.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.*

@Serializable
data class ArrangorflateTilsagn(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val gjennomforing: Gjennomforing,
    val gjenstaendeBelop: Int,
    val tiltakstype: Tiltakstype,
    val type: TilsagnType,
    val periode: Periode,
    val beregning: TilsagnBeregning,
    val arrangor: Arrangor,
    val status: StatusOgAarsaker,
) {
    @Serializable
    data class Arrangor(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val organisasjonsnummer: Organisasjonsnummer,
        val navn: String,
    )

    @Serializable
    data class Gjennomforing(
        val navn: String,
    )

    @Serializable
    data class Tiltakstype(
        val navn: String,
    )

    @Serializable
    data class StatusOgAarsaker(
        val status: TilsagnStatus,
        val aarsaker: List<TilsagnStatusAarsak>?,
    )
}
