package no.nav.mulighetsrommet.api.tilsagn.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetDbo
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDate
import java.time.LocalDateTime
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
    val arrangor: Arrangor,
    val tiltaksgjennomforing: Tiltaksgjennomforing,
    val status: TilsagnStatus,
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
    data class Tiltaksgjennomforing(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
    )

    @Serializable
    sealed class TilsagnStatus {
        @Serializable
        @SerialName("TIL_GODKJENNING")
        data class TilGodkjenning(
            val endretAv: NavIdent,
            @Serializable(with = LocalDateTimeSerializer::class)
            val endretTidspunkt: LocalDateTime,
        ) : TilsagnStatus()

        @Serializable
        @SerialName("GODKJENT")
        data object Godkjent : TilsagnStatus()

        @Serializable
        @SerialName("RETURNERT")
        data class Returnert(
            val endretAv: NavIdent,
            val returnertAv: NavIdent,
            val returnertAvNavn: String,
            val aarsaker: List<TilsagnStatusAarsak>,
            val forklaring: String?,
            @Serializable(with = LocalDateTimeSerializer::class)
            val endretTidspunkt: LocalDateTime,
        ) : TilsagnStatus()

        @Serializable
        @SerialName("TIL_ANNULLERING")
        data class TilAnnullering(
            val endretAv: NavIdent,
            val endretAvNavn: String,
            @Serializable(with = LocalDateTimeSerializer::class)
            val endretTidspunkt: LocalDateTime,
            val aarsaker: List<TilsagnStatusAarsak>,
            val forklaring: String?,
        ) : TilsagnStatus()

        @Serializable
        @SerialName("ANNULLERT")
        data class Annullert(
            val endretAv: NavIdent,
            val godkjentAv: NavIdent,
            @Serializable(with = LocalDateTimeSerializer::class)
            val endretTidspunkt: LocalDateTime,
            val aarsaker: List<TilsagnStatusAarsak>,
            val forklaring: String?,
        ) : TilsagnStatus()
    }
}
