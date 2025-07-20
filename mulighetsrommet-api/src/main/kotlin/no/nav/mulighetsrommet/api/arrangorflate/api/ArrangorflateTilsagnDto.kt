package no.nav.mulighetsrommet.api.arrangorflate.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnBeregningDto
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatusAarsak
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.*

@Serializable
data class ArrangorflateTilsagnDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val gjennomforing: Gjennomforing,
    val bruktBelop: Int,
    val gjenstaendeBelop: Int,
    val tiltakstype: Tiltakstype,
    val type: TilsagnType,
    val periode: Periode,
    val beregning: TilsagnBeregningDto,
    val arrangor: Arrangor,
    val status: StatusOgAarsaker,
    val bestillingsnummer: String,
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
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
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
