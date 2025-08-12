package no.nav.mulighetsrommet.api.utbetaling.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.serializers.AgentSerializer
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.tiltak.okonomi.Tilskuddstype
import java.time.LocalDateTime
import java.util.*

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
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    val beskrivelse: String?,
    val begrunnelseMindreBetalt: String?,
    val tilskuddstype: Tilskuddstype,
    val status: UtbetalingStatus,
    val avbrutt: Avbrutt?,
) {
    enum class UtbetalingStatus {
        /**
         * Systemet har generert utbetalingen, men den er enda ikke godkjent av arrangør.
         */
        OPPRETTET,

        /**
         * Arrangør eller Nav-ansatt har opprettet utbetalingen.
         */
        INNSENDT,

        /**
         * Saksbehandler hos Nav har utført kostnadsfordeling og sendt utbetalingen til attestering.
         */
        TIL_ATTESTERING,

        /**
         * Attestant har sendt utbetalingen i retur.
         */
        RETURNERT,

        /**
         * Attestant har godkjent (attestert) utbetalingen.
         */
        FERDIG_BEHANDLET,

        /**
         * Utbetalingen ble avbrutt (ikke utbetalt).
         */
        AVBRUTT,
    }

    @Serializable
    data class Gjennomforing(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
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

    @Serializable
    data class Avbrutt(
        val aarsaker: List<String>,
        val forklaring: String?,
        @Serializable(with = LocalDateTimeSerializer::class)
        val tidspunkt: LocalDateTime,
    )
}
