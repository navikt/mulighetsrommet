package no.nav.mulighetsrommet.api.utbetaling.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltaksnummer
import no.nav.mulighetsrommet.serializers.AgentSerializer
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
    @Serializable(with = AgentSerializer::class)
    val innsender: Agent?,
    val tiltakstype: Tiltakstype,
    val gjennomforing: Gjennomforing,
    val arrangor: Arrangor,
    val beregning: UtbetalingBeregning,
    val betalingsinformasjon: Betalingsinformasjon,
    val journalpostId: String?,
    val periode: Periode,
    @Serializable(with = LocalDateTimeSerializer::class)
    val godkjentAvArrangorTidspunkt: LocalDateTime?,
    @Serializable(with = InstantSerializer::class)
    val utbetalesTidligstTidspunkt: Instant?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    val beskrivelse: String?,
    val begrunnelseMindreBetalt: String?,
    val tilskuddstype: Tilskuddstype,
    val status: UtbetalingStatusType,
    val avbruttBegrunnelse: String?,
) {
    @Serializable
    data class Gjennomforing(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val lopenummer: Tiltaksnummer,
        val navn: String,
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
    data class Betalingsinformasjon(
        val kontonummer: Kontonummer?,
        val kid: Kid?,
    )
}
