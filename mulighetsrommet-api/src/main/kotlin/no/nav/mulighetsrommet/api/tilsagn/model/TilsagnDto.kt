package no.nav.mulighetsrommet.api.tilsagn.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetDbo
import no.nav.mulighetsrommet.api.okonomi.Prismodell
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
    @Serializable(with = LocalDateSerializer::class)
    val periodeStart: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val periodeSlutt: LocalDate,
    val opprettetAv: NavIdent,
    val kostnadssted: NavEnhetDbo,
    val beregning: Prismodell.TilsagnBeregning,
    @Serializable(with = LocalDateTimeSerializer::class)
    val annullertTidspunkt: LocalDateTime?,
    val lopenummer: Int,
    val arrangor: Arrangor,
    val besluttelse: Besluttelse?,
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
    data class Besluttelse(
        val navIdent: NavIdent,
        val beslutternavn: String,
        @Serializable(with = LocalDateTimeSerializer::class)
        val tidspunkt: LocalDateTime?,
        val status: TilsagnBesluttelseStatus,
        val aarsaker: List<AvvistTilsagnAarsak>?,
        val forklaring: String?,
    )

    @Serializable
    data class Tiltaksgjennomforing(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
    )

    @Serializable
    enum class TilsagnStatus {
        TIL_GODKJENNING,
        GODKJENT,
        RETURNERT,
        ANNULLERT,
        OPPGJORT,
    }
}
