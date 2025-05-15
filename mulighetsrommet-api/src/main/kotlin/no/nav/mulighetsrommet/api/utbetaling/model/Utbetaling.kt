package no.nav.mulighetsrommet.api.utbetaling.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.serializers.AgentSerializer
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.tiltak.okonomi.Tilskuddstype
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Serializable
data class Utbetaling(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = AgentSerializer::class)
    val innsender: Agent?,
    @Serializable(with = LocalDateSerializer::class)
    val fristForGodkjenning: LocalDate,
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
    val tilskuddstype: Tilskuddstype,
) {
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
}
