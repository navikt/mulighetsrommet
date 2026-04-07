package no.nav.mulighetsrommet.api.utbetaling.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.arrangor.model.Betalingsinformasjon
import no.nav.mulighetsrommet.model.JournalpostId
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltaksnummer
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.serializers.InstantSerializer
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.tiltak.okonomi.Tilskuddstype
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class Utbetaling(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val tiltakstype: Tiltakstype,
    val gjennomforing: Gjennomforing,
    val arrangor: Arrangor,
    val korreksjon: Korreksjon?,
    val innsending: Innsending?,
    val valuta: Valuta,
    val beregning: UtbetalingBeregning,
    val betalingsinformasjon: Betalingsinformasjon?,
    val journalpostId: JournalpostId?,
    val periode: Periode,
    @Serializable(with = InstantSerializer::class)
    val utbetalesTidligstTidspunkt: Instant?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime,
    val kommentar: String?,
    val begrunnelseMindreBetalt: String?,
    val tilskuddstype: Tilskuddstype,
    val status: UtbetalingStatusType,
    val avbruttBegrunnelse: String?,
    @Serializable(with = InstantSerializer::class)
    val avbruttTidspunkt: Instant?,
    val blokkeringer: Set<Blokkering>,
) {
    fun arrangorInnsendtAnnenAvtaltPris(): Boolean {
        return when (beregning) {
            is UtbetalingBeregningFastSatsPerTiltaksplassPerManed,
            is UtbetalingBeregningPrisPerHeleUkesverk,
            is UtbetalingBeregningPrisPerManedsverk,
            is UtbetalingBeregningPrisPerTimeOppfolging,
            is UtbetalingBeregningPrisPerUkesverk,
            -> false

            is UtbetalingBeregningFri -> innsending != null
        }
    }

    @Serializable
    data class Gjennomforing(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val lopenummer: Tiltaksnummer,
    )

    @Serializable
    data class Arrangor(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val organisasjonsnummer: Organisasjonsnummer,
        val navn: String,
        val slettet: Boolean,
    )

    @Serializable
    data class Tiltakstype(
        val navn: String,
        val tiltakskode: Tiltakskode,
    )

    @Serializable
    data class Korreksjon(
        @Serializable(with = UUIDSerializer::class)
        val gjelderUtbetalingId: UUID,
        val begrunnelse: String,
    )

    @Serializable
    data class Innsending(
        @Serializable(with = LocalDateTimeSerializer::class)
        val tidspunkt: LocalDateTime,
    )

    @Serializable
    enum class Blokkering {
        UBEHANDLET_FORSLAG,
    }
}
