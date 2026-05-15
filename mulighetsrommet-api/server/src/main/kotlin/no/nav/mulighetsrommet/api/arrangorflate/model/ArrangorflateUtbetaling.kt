package no.nav.mulighetsrommet.api.arrangorflate.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.arrangor.Betalingsinformasjon
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregning
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerHeleUke
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerManed
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerUke
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningAvtaltPrisPerTimeOppfolging
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFastSatsPerAvtaltTiltaksplassPerManed
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFastSatsPerBenyttetPlassPerManed
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltaksnummer
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.serializers.InstantSerializer
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.tiltak.okonomi.Tilskuddstype
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class ArrangorflateUtbetaling(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val tiltakstype: Tiltakstype,
    val gjennomforing: Gjennomforing,
    val arrangor: Arrangor,
    val korreksjon: Korreksjon?,
    val innsending: Innsending?,
    val valuta: Valuta,
    val beregning: UtbetalingBeregning,
    val betalingsinformasjon: Betalingsinformasjon.BBan?,
    val periode: Periode,
    @Serializable(with = InstantSerializer::class)
    val utbetalesTidligstTidspunkt: Instant?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime,
    val tilskuddstype: Tilskuddstype,
    val status: UtbetalingStatusType,
    @Serializable(with = InstantSerializer::class)
    val avbruttTidspunkt: Instant?,
    val blokkeringer: Set<Utbetaling.Blokkering>,
) {
    fun arrangorInnsendtAnnenAvtaltPris(): Boolean {
        return when (beregning) {
            is UtbetalingBeregningFastSatsPerBenyttetPlassPerManed,
            is UtbetalingBeregningFastSatsPerAvtaltTiltaksplassPerManed,
            is UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerHeleUke,
            is UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerManed,
            is UtbetalingBeregningAvtaltPrisPerTimeOppfolging,
            is UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerUke,
            -> false

            is UtbetalingBeregningFri -> innsending != null
        }
    }

    @Serializable
    data class Gjennomforing(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val navn: String,
        val lopenummer: Tiltaksnummer,
        @Serializable(with = LocalDateSerializer::class)
        val startDato: LocalDate,
        @Serializable(with = LocalDateSerializer::class)
        val sluttDato: LocalDate?,
    )

    @Serializable
    data class Arrangor(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val organisasjonsnummer: Organisasjonsnummer,
        val navn: String,
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
}
