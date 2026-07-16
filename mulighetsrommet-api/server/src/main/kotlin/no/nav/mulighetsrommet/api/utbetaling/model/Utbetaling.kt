package no.nav.mulighetsrommet.api.utbetaling.model

import arrow.core.Either
import arrow.core.left
import arrow.core.nel
import arrow.core.right
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.arrangor.Betalingsinformasjon
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.Totrinnskontroll
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.totrinnskontroll.api.toFieldErrors
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.JournalpostId
import no.nav.mulighetsrommet.model.NavIdent
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
    val avbrytelse: Tilstandsendring?,
) {
    /**
     * Representerer en endring av status etter en totrinnskontroll
     *
     * Indousert ved avbrytelse via tiltaksadmin, da en utbetaling kan avbrytes fra statusene GENERERT, TIL_BEHANDLING og RETURNERT
     *
     * Siden utbetalingens status er TIL_AVBRYTELSE, vil dette hjelpe oss med å gå til rett status avhengig av totrinnskontrollen
     */
    @Serializable
    data class Tilstandsendring(
        val totrinnskontroll: Totrinnskontroll,
        val returnert: UtbetalingStatusType,
        val godkjent: UtbetalingStatusType,
    )

    fun settTilAbrytelse(
        agent: Agent,
        aarsaker: List<String>,
        forklaring: String?,
    ): Either<List<FieldError>, Utbetaling> {
        if (!kanSettesTilAvbrytelse()) {
            return FieldError.of("Utbetaling kan ikke settes til avbrytelse").nel().left()
        }
        return copy(
            status = UtbetalingStatusType.TIL_AVBRYTELSE,
            avbrytelse = Tilstandsendring(
                totrinnskontroll = Totrinnskontroll.opprett(
                    id = UUID.randomUUID(),
                    entityId = id,
                    type = TotrinnskontrollType.UTBETALING_AVBRYTELSE,
                    behandletAv = agent,
                    aarsaker = aarsaker,
                    forklaring = forklaring,
                ),
                returnert = status,
                godkjent = UtbetalingStatusType.AVBRUTT,
            ),
        ).right()
    }

    fun godkjennAvbrytelse(agent: Agent): Either<List<FieldError>, Utbetaling> {
        if (status != UtbetalingStatusType.TIL_AVBRYTELSE) {
            return FieldError.of("Utbetalingen kan ikke avbrytes")
                .nel()
                .left()
        }
        return avbrytelse!!.totrinnskontroll.godkjenn(agent).mapLeft { it.toFieldErrors() }.map { godkjent ->
            copy(avbrytelse = avbrytelse.copy(totrinnskontroll = godkjent), status = avbrytelse.godkjent)
        }
    }

    fun avslaAbrytelse(
        besluttetAv: NavIdent,
        aarsaker: List<String>,
        forklaring: String?,
    ): Either<List<FieldError>, Utbetaling> {
        if (status != UtbetalingStatusType.TIL_AVBRYTELSE) {
            return FieldError.of("Utbetalingen kan ikke avbrytes")
                .nel()
                .left()
        }
        return avbrytelse!!.totrinnskontroll.returner(besluttetAv, aarsaker, forklaring).mapLeft { it.toFieldErrors() }
            .map { retunert ->
                copy(avbrytelse = avbrytelse.copy(totrinnskontroll = retunert), status = avbrytelse.returnert)
            }
    }

    fun arrangorInnsendtAnnenAvtaltPris(): Boolean {
        return when (beregning) {
            is UtbetalingBeregningFastSatsPerTiltaksplassPerManed,
            is UtbetalingBeregningFastSatsPerAvtaltTiltaksplassPerManed,
            is UtbetalingBeregningPrisPerHeleUkesverk,
            is UtbetalingBeregningPrisPerManedsverk,
            is UtbetalingBeregningPrisPerTimeOppfolging,
            is UtbetalingBeregningPrisPerUkesverk,
            -> false

            is UtbetalingBeregningFri -> innsending != null
        }
    }

    fun erTilBehandling(): Boolean = when (status) {
        UtbetalingStatusType.TIL_BEHANDLING,
        UtbetalingStatusType.RETURNERT,
        -> true

        UtbetalingStatusType.GENERERT,
        UtbetalingStatusType.TIL_ATTESTERING,
        UtbetalingStatusType.FERDIG_BEHANDLET,
        UtbetalingStatusType.DELVIS_UTBETALT,
        UtbetalingStatusType.UTBETALT,
        UtbetalingStatusType.TIL_AVBRYTELSE,
        UtbetalingStatusType.AVBRUTT,
        -> false
    }

    fun erFerdigBehandlet(): Boolean = when (status) {
        UtbetalingStatusType.FERDIG_BEHANDLET,
        UtbetalingStatusType.DELVIS_UTBETALT,
        UtbetalingStatusType.UTBETALT,
        -> true

        UtbetalingStatusType.RETURNERT,
        UtbetalingStatusType.TIL_BEHANDLING,
        UtbetalingStatusType.GENERERT,
        UtbetalingStatusType.TIL_ATTESTERING,
        UtbetalingStatusType.TIL_AVBRYTELSE,
        UtbetalingStatusType.AVBRUTT,
        -> false
    }

    // TODO: sealed class i stedet for nullable properties?
    fun erInnsending(): Boolean = innsending != null

    fun erKorreksjon(): Boolean = korreksjon != null

    fun kanSettesTilAvbrytelse(): Boolean = !erKorreksjon() && when (status) {
        UtbetalingStatusType.GENERERT,
        UtbetalingStatusType.TIL_BEHANDLING,
        UtbetalingStatusType.RETURNERT,
        ->
            true

        UtbetalingStatusType.TIL_AVBRYTELSE,
        UtbetalingStatusType.AVBRUTT,
        UtbetalingStatusType.FERDIG_BEHANDLET,
        UtbetalingStatusType.UTBETALT,
        UtbetalingStatusType.DELVIS_UTBETALT,
        UtbetalingStatusType.TIL_ATTESTERING,
        ->
            false
    }

    fun getTiltaksnavn(): String {
        return "${tiltakstype.navn} (${gjennomforing.lopenummer.value})"
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
