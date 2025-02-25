package no.nav.mulighetsrommet.api.tilsagn.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetDbo
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.*

@Serializable
data class TilsagnDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val type: TilsagnType,
    @Serializable(with = LocalDateSerializer::class)
    val periodeStart: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val periodeSlutt: LocalDate,
    val kostnadssted: NavEnhetDbo,
    val beregning: TilsagnBeregning,
    val lopenummer: Int,
    val bestillingsnummer: String,
    val arrangor: Arrangor,
    val gjennomforing: Gjennomforing,
    val status: TilsagnStatusDto,
) {
    @Serializable
    data class Arrangor(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val organisasjonsnummer: Organisasjonsnummer,
        val navn: String,
        val slettet: Boolean,
    )

    @Serializable
    data class Gjennomforing(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val tiltakskode: Tiltakskode,
        val navn: String,
    )

    @Serializable
    sealed class TilsagnStatusDto {
        abstract val opprettelse: Totrinnskontroll

        @Serializable
        @SerialName("TIL_GODKJENNING")
        data class TilGodkjenning(
            override val opprettelse: Totrinnskontroll.Ubesluttet,
        ) : TilsagnStatusDto()

        @Serializable
        @SerialName("GODKJENT")
        data class Godkjent(
            override val opprettelse: Totrinnskontroll.Besluttet,
        ) : TilsagnStatusDto()

        @Serializable
        @SerialName("RETURNERT")
        data class Returnert(
            override val opprettelse: Totrinnskontroll.Besluttet,
        ) : TilsagnStatusDto()

        @Serializable
        @SerialName("TIL_ANNULLERING")
        data class TilAnnullering(
            override val opprettelse: Totrinnskontroll.Besluttet,
            val annullering: Totrinnskontroll.Ubesluttet,
        ) : TilsagnStatusDto()

        @Serializable
        @SerialName("ANNULLERT")
        data class Annullert(
            override val opprettelse: Totrinnskontroll.Besluttet,
            val annullering: Totrinnskontroll.Besluttet,
        ) : TilsagnStatusDto()
    }
}
