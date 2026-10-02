package no.nav.mulighetsrommet.api.tilsagn

import arrow.core.Either
import arrow.core.left
import arrow.core.nel
import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import arrow.core.right
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.admin.endringshistorikk.EndringshistorikkType
import no.nav.mulighetsrommet.admin.navansatt.service.NavAnsattService
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.TransactionalQueryContext
import no.nav.mulighetsrommet.api.aarsakerbegrunnelse.AarsakerOgBegrunnelseRequest
import no.nav.mulighetsrommet.api.domain.arrangor.Arrangor
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.Totrinnskontroll
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingType
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtale
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingEnkeltplass
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingTiltaksadministrasjon
import no.nav.mulighetsrommet.api.navansatt.OkonomiAuthorization
import no.nav.mulighetsrommet.api.navansatt.OkonomiBeslutningContext
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnDbo
import no.nav.mulighetsrommet.api.tilsagn.model.BeregnTilsagnRequest
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregning
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningAnnenAvtaltPris
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningAvtaltPrisPerBenyttetPlassPerHeleUke
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningAvtaltPrisPerBenyttetPlassPerManed
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningAvtaltPrisPerBenyttetPlassPerUke
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningAvtaltPrisPerTimeOppfolgingPerDeltaker
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFastSatsPerBenyttetPlassPerManed
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFri
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningType
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnRequest
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatusAarsak
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.api.totrinnskontroll.api.toFieldErrors
import no.nav.mulighetsrommet.api.utbetaling.model.StengtPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingInputHelper
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.Arena
import no.nav.mulighetsrommet.model.FieldError
import no.nav.mulighetsrommet.model.NOK
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltaksadministrasjon
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.model.withValuta
import no.nav.mulighetsrommet.notifications.NotificationMetadata
import no.nav.mulighetsrommet.notifications.ScheduledNotification
import no.nav.tiltak.okonomi.AnnullerBestilling
import no.nav.tiltak.okonomi.Bestillingsnummer
import no.nav.tiltak.okonomi.GjorOppBestilling
import no.nav.tiltak.okonomi.OkonomiBestillingMelding
import no.nav.tiltak.okonomi.OpprettBestilling
import no.nav.tiltak.okonomi.Tilskuddstype
import no.nav.tiltak.okonomi.toOkonomiPart
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

class TilsagnService(
    val config: Config,
    private val db: ApiDatabase,
    private val navAnsattService: NavAnsattService,
) {
    data class Config(
        val gyldigTilsagnPeriode: Map<Tiltakskode, Periode>,
    )

    fun upsert(request: TilsagnRequest, agent: Agent): Either<TilsagnError, Tilsagn> = db.transaction {
        upsertInTx(request, agent)
    }

    context(tx: TransactionalQueryContext)
    fun upsertInTx(request: TilsagnRequest, agent: Agent): Either<TilsagnError, Tilsagn> = with(tx) {
        requireNotNull(request.id) { "id mangler" }

        val gjennomforing = queries.gjennomforing.getGjennomforingTiltaksadministrasjon(request.gjennomforingId)
        if (!kanOppretteTilsagn(agent, gjennomforing)) {
            val message = when (agent) {
                is NavIdent -> "Du mangler saksbehandlertilgang til tilsagnet"
                else -> "$agent kan ikke opprette tilsagn"
            }
            return TilsagnError.ManglerTilgang(message).left()
        }

        val previous = queries.tilsagn.getAndAcquireLockOrNull(request.id)
        val stengt = when (gjennomforing) {
            is GjennomforingAvtale -> gjennomforing.stengt
            is GjennomforingEnkeltplass -> listOf()
        }
        return TilsagnValidator
            .validate(
                next = request,
                previous = previous,
                tiltakstypeNavn = gjennomforing.tiltakstype.navn,
                arrangorSlettet = gjennomforing.arrangor.slettet,
                gyldigTilsagnPeriode = config.gyldigTilsagnPeriode[gjennomforing.tiltakstype.tiltakskode],
                gjennomforingSluttDato = gjennomforing.sluttDato,
                prismodell = gjennomforing.prismodell,
                stengt = stengt,
            )
            .mapLeft(TilsagnError::Valideringsfeil)
            .map { validated ->
                val lopenummer = previous?.lopenummer
                    ?: queries.tilsagn.getNextLopenummeByGjennomforing(gjennomforing.id)

                val bestillingsnummer = previous?.bestilling?.bestillingsnummer
                    ?: "A-${gjennomforing.lopenummer.value}-$lopenummer"

                TilsagnDbo(
                    id = request.id,
                    gjennomforingId = request.gjennomforingId,
                    type = request.type,
                    periode = validated.periode,
                    lopenummer = lopenummer,
                    kostnadssted = validated.kostnadssted,
                    bestillingsnummer = bestillingsnummer,
                    bestillingStatus = null,
                    belopBrukt = 0.withValuta(gjennomforing.prismodell.valuta),
                    beregning = validated.beregning,
                    kommentar = request.kommentar?.trim(),
                    beskrivelse = request.beskrivelse?.trim(),
                    deltakere = request.deltakere?.map {
                        TilsagnDbo.Deltaker(it.deltakerId, it.innholdAnnet)
                    },
                )
            }
            .map { dbo ->
                queries.tilsagn.upsert(dbo)
                val opprettelse = Totrinnskontroll.opprett(
                    UUID.randomUUID(),
                    dbo.id,
                    TotrinnskontrollType.TILSAGN_OPPRETTELSE,
                    agent,
                )
                queries.totrinnskontroll.upsert(opprettelse)
                outbox.publish(opprettelse)
                logEndring("Sendt til godkjenning", dbo.id, agent).also {
                    updateFreeTextSearch(dbo)
                }
            }
    }

    fun slettTilsagn(id: UUID, navIdent: NavIdent): Either<TilsagnError, Unit> = db.transaction {
        val tilsagn = queries.tilsagn.getAndAcquireLock(id)
        if (!erSaksbehandler(navIdent, tilsagn)) {
            return TilsagnError.ManglerTilgang("Du mangler saksbehandlertilgang til tilsagnet").left()
        }
        if (tilsagn.status != TilsagnStatus.RETURNERT) {
            return TilsagnError.Valideringsfeil(FieldError.of("Kan ikke slette tilsagn som er godkjent").nel()).left()
        }

        val opprettelse = queries.totrinnskontroll.getOrError(id, TotrinnskontrollType.TILSAGN_OPPRETTELSE)
        val behandletAv = opprettelse.behandling.utfortAv
        if (opprettelse.beslutning?.utfortAv == navIdent && behandletAv is NavIdent) {
            sendNotifikasjonSlettetTilsagn(tilsagn, besluttetAv = navIdent, behandletAv = behandletAv)
        }

        queries.tilsagn.delete(id)

        Unit.right()
    }

    fun tilAnnulleringRequest(
        id: UUID,
        navIdent: NavIdent,
        request: AarsakerOgBegrunnelseRequest<TilsagnStatusAarsak>,
    ): Either<TilsagnError, Tilsagn> = db.transaction {
        val tilsagn = queries.tilsagn.getAndAcquireLock(id)
        if (!erSaksbehandler(navIdent, tilsagn)) {
            return TilsagnError.ManglerTilgang("Du mangler saksbehandlertilgang til tilsagnet").left()
        }
        if (tilsagn.status != TilsagnStatus.GODKJENT) {
            return TilsagnError.Valideringsfeil(FieldError.of("Kan bare annullere godkjente tilsagn").nel()).left()
        }
        setTilAnnullering(tilsagn, navIdent, request.aarsaker.map { it.name }, request.begrunnelse).right()
    }

    fun tilOppgjorRequest(
        id: UUID,
        navIdent: NavIdent,
        request: AarsakerOgBegrunnelseRequest<TilsagnStatusAarsak>,
    ): Either<TilsagnError, Tilsagn> = db.transaction {
        val tilsagn = queries.tilsagn.getAndAcquireLock(id)
        if (!erSaksbehandler(navIdent, tilsagn)) {
            return TilsagnError.ManglerTilgang("Du mangler saksbehandlertilgang til tilsagnet").left()
        }
        if (tilsagn.status != TilsagnStatus.GODKJENT) {
            return TilsagnError.Valideringsfeil(FieldError.of("Kan bare gjøre opp godkjente tilsagn").nel()).left()
        }
        setTilOppgjor(
            tilsagn,
            navIdent,
            aarsaker = request.aarsaker.map { it.name },
            begrunnelse = request.begrunnelse,
            operation = "Sendt til oppgjør",
        ).right()
    }

    fun beregnTilsagnUnvalidated(request: BeregnTilsagnRequest): TilsagnBeregning? = db.session {
        return try {
            when (request.beregning.type) {
                TilsagnBeregningType.ANNEN_AVTALT_PRIS ->
                    TilsagnBeregningAnnenAvtaltPris.beregn(
                        TilsagnBeregningAnnenAvtaltPris.Input(
                            linjer = request.beregning.linjer.orEmpty().map {
                                TilsagnBeregningAnnenAvtaltPris.InputLinje(
                                    id = it.id,
                                    beskrivelse = it.beskrivelse ?: "",
                                    pris = it.pris ?: 0.NOK,
                                    antall = it.antall ?: 0,
                                )
                            },
                            prisbetingelser = request.beregning.prisbetingelser,
                        ),
                    )

                TilsagnBeregningType.FRI ->
                    TilsagnBeregningFri.beregn(
                        TilsagnBeregningFri.Input(
                            pris = request.beregning.pris ?: 0.NOK,
                        ),
                    )

                TilsagnBeregningType.FAST_SATS_PER_TILTAKSPLASS_PER_MANED ->
                    beregnTilsagnFallbackResolver(request)?.let { fallback ->
                        TilsagnBeregningFastSatsPerBenyttetPlassPerManed.beregn(
                            TilsagnBeregningFastSatsPerBenyttetPlassPerManed.Input(
                                periode = fallback.periode,
                                sats = fallback.sats,
                                antallPlasser = fallback.antallPlasser,
                                stengt = fallback.stengt,
                            ),
                        )
                    }

                TilsagnBeregningType.PRIS_PER_MANEDSVERK ->
                    beregnTilsagnFallbackResolver(request)?.let { fallback ->
                        TilsagnBeregningAvtaltPrisPerBenyttetPlassPerManed.beregn(
                            TilsagnBeregningAvtaltPrisPerBenyttetPlassPerManed.Input(
                                periode = fallback.periode,
                                sats = fallback.sats,
                                antallPlasser = fallback.antallPlasser,
                                prisbetingelser = fallback.prisbetingelser,
                                stengt = fallback.stengt,
                            ),
                        )
                    }

                TilsagnBeregningType.PRIS_PER_UKESVERK ->
                    beregnTilsagnFallbackResolver(request)?.let { fallback ->
                        TilsagnBeregningAvtaltPrisPerBenyttetPlassPerUke.beregn(
                            TilsagnBeregningAvtaltPrisPerBenyttetPlassPerUke.Input(
                                periode = fallback.periode,
                                sats = fallback.sats,
                                antallPlasser = fallback.antallPlasser,
                                prisbetingelser = fallback.prisbetingelser,
                                stengt = fallback.stengt,
                            ),
                        )
                    }

                TilsagnBeregningType.PRIS_PER_HELE_UKESVERK ->
                    beregnTilsagnFallbackResolver(request)?.let { fallback ->
                        TilsagnBeregningAvtaltPrisPerBenyttetPlassPerHeleUke.beregn(
                            TilsagnBeregningAvtaltPrisPerBenyttetPlassPerHeleUke.Input(
                                periode = fallback.periode,
                                sats = fallback.sats,
                                antallPlasser = fallback.antallPlasser,
                                prisbetingelser = fallback.prisbetingelser,
                                stengt = fallback.stengt,
                            ),
                        )
                    }

                TilsagnBeregningType.PRIS_PER_TIME_OPPFOLGING ->
                    beregnTilsagnFallbackResolver(request)?.let { fallback ->
                        TilsagnBeregningAvtaltPrisPerTimeOppfolgingPerDeltaker.beregn(
                            TilsagnBeregningAvtaltPrisPerTimeOppfolgingPerDeltaker.Input(
                                periode = fallback.periode,
                                sats = fallback.sats,
                                antallPlasser = fallback.antallPlasser,
                                prisbetingelser = fallback.prisbetingelser,
                                antallTimerOppfolgingPerDeltaker = fallback.antallTimerOppfolgingPerDeltaker,
                            ),
                        )
                    }
            }
        } catch (_: Throwable) {
            null
        }
    }

    private data class TilsagnBeregningFallbackResolver(
        val sats: ValutaBelop,
        val periode: Periode,
        val antallPlasser: Int,
        val antallTimerOppfolgingPerDeltaker: Int,
        val prisbetingelser: String?,
        val stengt: Set<StengtPeriode>,
    )

    private fun beregnTilsagnFallbackResolver(request: BeregnTilsagnRequest): TilsagnBeregningFallbackResolver? = db.session {
        if (request.periodeStart == null || request.periodeSlutt == null || !request.periodeStart.isBefore(request.periodeSlutt)) {
            return null
        }

        val gjennomforing = queries.gjennomforing.getGjennomforingTiltaksadministrasjon(request.gjennomforingId)
        val avtaltSats = gjennomforing.prismodell.findAvtaltSats(request.periodeStart)

        val antallPlasserFallback = request.beregning.antallPlasser ?: 0
        val antallTimerOppfolgingPerDeltakerFallback = request.beregning.antallTimerOppfolgingPerDeltaker ?: 0
        val periode = Periode.fromInclusiveDates(request.periodeStart, request.periodeSlutt)

        val stengt = when (gjennomforing) {
            is GjennomforingAvtale -> UtbetalingInputHelper.resolveStengtHosArrangor(periode, gjennomforing.stengt)
            is GjennomforingEnkeltplass -> setOf()
        }

        return TilsagnBeregningFallbackResolver(
            sats = avtaltSats?.sats ?: ValutaBelop(0, Valuta.NOK),
            periode = periode,
            antallPlasser = antallPlasserFallback,
            antallTimerOppfolgingPerDeltaker = antallTimerOppfolgingPerDeltakerFallback,
            prisbetingelser = request.beregning.prisbetingelser,
            stengt = stengt,
        )
    }

    fun godkjennTilsagn(
        id: UUID,
        agent: Agent,
    ): Either<TilsagnError, Tilsagn> = db.transaction { godkjennTilsagnInTx(id, agent) }

    context(tx: TransactionalQueryContext)
    fun godkjennTilsagnInTx(
        id: UUID,
        agent: Agent,
    ): Either<TilsagnError, Tilsagn> = with(tx) {
        val tilsagn = queries.tilsagn.getAndAcquireLock(id)

        if (!kanBeslutteTilsagn(agent, tilsagn)) {
            val message = when (agent) {
                is NavIdent -> "Du kan ikke beslutte tilsagnet fordi du mangler budsjettmyndighet ved tilsagnets kostnadssted (${tilsagn.kostnadssted.navn})"
                else -> "$agent kan ikke beslutte tilsagn"
            }
            return TilsagnError.ManglerTilgang(message).left()
        }

        when (tilsagn.status) {
            TilsagnStatus.OPPGJORT, TilsagnStatus.ANNULLERT, TilsagnStatus.GODKJENT, TilsagnStatus.RETURNERT,
            -> FieldError.of("Tilsagnet kan ikke godkjennes fordi det har status ${tilsagn.status.beskrivelse}")
                .nel()
                .left()

            TilsagnStatus.TIL_GODKJENNING -> godkjennTilsagn(tilsagn, agent).onRight {
                publishOpprettBestilling(it)
            }

            TilsagnStatus.TIL_ANNULLERING -> annullerTilsagn(tilsagn, agent).onRight {
                publishAnnullerBestilling(it)
            }

            TilsagnStatus.TIL_OPPGJOR -> gjorOppTilsagn(tilsagn, agent, "Tilsagn oppgjort").onRight {
                publishGjorOppBestilling(it)
            }
        }.mapLeft(TilsagnError::Valideringsfeil)
    }

    fun returnerTilsagn(
        id: UUID,
        navIdent: NavIdent,
        aarsaker: List<TilsagnStatusAarsak>,
        begrunnelse: String?,
    ): Either<TilsagnError, Tilsagn> = db.transaction {
        val tilsagn = queries.tilsagn.getAndAcquireLock(id)

        if (!erSaksbehandler(navIdent, tilsagn) && !erBeslutter(navIdent, tilsagn)) {
            return TilsagnError.ManglerTilgang("Du kan ikke returnere tilsagnet fordi du mangler tilgang").left()
        }

        when (tilsagn.status) {
            TilsagnStatus.OPPGJORT, TilsagnStatus.ANNULLERT, TilsagnStatus.GODKJENT, TilsagnStatus.RETURNERT,
            -> FieldError.of("Tilsagnet kan ikke returneres fordi det har status ${tilsagn.status.beskrivelse}")
                .nel()
                .left()

            TilsagnStatus.TIL_GODKJENNING -> returnerTilsagn(tilsagn, navIdent, aarsaker, begrunnelse)

            TilsagnStatus.TIL_ANNULLERING -> avvisAnnullering(tilsagn, navIdent, aarsaker, begrunnelse)

            TilsagnStatus.TIL_OPPGJOR -> avvisOppgjor(tilsagn, navIdent, aarsaker, begrunnelse)
        }.mapLeft(TilsagnError::Valideringsfeil)
    }

    fun republishOpprettBestilling(bestillingsnummer: String): Tilsagn = db.transaction {
        val tilsagn = queries.tilsagn.getOrError(bestillingsnummer)
        publishOpprettBestilling(tilsagn)
        tilsagn
    }

    private fun TransactionalQueryContext.godkjennTilsagn(
        tilsagn: Tilsagn,
        besluttetAv: Agent,
    ): Either<List<FieldError>, Tilsagn> {
        if (tilsagn.status != TilsagnStatus.TIL_GODKJENNING) {
            return FieldError.of("Tilsagnet må ha status ${TilsagnStatus.TIL_GODKJENNING} for å godkjennes")
                .nel()
                .left()
        }

        val opprettelse = queries.totrinnskontroll.getOrError(tilsagn.id, TotrinnskontrollType.TILSAGN_OPPRETTELSE)
        return opprettelse.godkjenn(besluttetAv).mapLeft { it.toFieldErrors() }.map { godkjent ->
            queries.totrinnskontroll.upsert(godkjent)
            outbox.publish(godkjent)
            queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.GODKJENT)
            logEndring("Tilsagn godkjent", tilsagn.id, besluttetAv)
        }
    }

    private fun TransactionalQueryContext.returnerTilsagn(
        tilsagn: Tilsagn,
        besluttetAv: NavIdent,
        aarsaker: List<TilsagnStatusAarsak>,
        begrunnelse: String?,
    ): Either<List<FieldError>, Tilsagn> {
        if (tilsagn.status != TilsagnStatus.TIL_GODKJENNING) {
            return FieldError.of("Tilsagnet må ha status ${TilsagnStatus.TIL_GODKJENNING} for å returneres")
                .nel()
                .left()
        }

        if (aarsaker.isEmpty()) {
            return FieldError.of("Årsaker er påkrevd").nel().left()
        }

        val opprettelse = queries.totrinnskontroll.getOrError(tilsagn.id, TotrinnskontrollType.TILSAGN_OPPRETTELSE)
        return opprettelse
            .returner(besluttetAv, begrunnelse, aarsaker.map { it.name })
            .mapLeft { it.toFieldErrors() }
            .map { returnert ->
                queries.totrinnskontroll.upsert(returnert)
                outbox.publish(returnert)
                queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.RETURNERT)
                logEndring("Tilsagn returnert", tilsagn.id, besluttetAv)
            }
    }

    private fun TransactionalQueryContext.setTilAnnullering(
        tilsagn: Tilsagn,
        behandletAv: Agent,
        aarsaker: List<String>,
        begrunnelse: String?,
    ): Tilsagn {
        require(tilsagn.status == TilsagnStatus.GODKJENT) {
            "Kan bare annullere godkjente tilsagn"
        }

        val annullering = Totrinnskontroll.opprett(
            id = UUID.randomUUID(),
            entityId = tilsagn.id,
            type = TotrinnskontrollType.TILSAGN_ANNULLERING,
            behandletAv = behandletAv,
            behandletBegrunnelse = begrunnelse,
            behandletAarsaker = aarsaker,
        )
        queries.totrinnskontroll.upsert(annullering)
        outbox.publish(annullering)
        queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.TIL_ANNULLERING)

        return logEndring("Sendt til annullering", tilsagn.id, behandletAv)
    }

    private fun TransactionalQueryContext.annullerTilsagn(
        tilsagn: Tilsagn,
        besluttetAv: Agent,
    ): Either<List<FieldError>, Tilsagn> {
        if (tilsagn.status != TilsagnStatus.TIL_ANNULLERING) {
            return FieldError.of("Tilsagnet må ha status ${TilsagnStatus.TIL_ANNULLERING} for at annullering skal godkjennes")
                .nel()
                .left()
        }
        if (db.session { queries.utbetalingLinje.getNextLopenummerByTilsagn(tilsagn.id) } > 1) {
            return FieldError.of("Tilsagnet kan ikke annulleres fordi det har blitt brukt i utbetalinger")
                .nel()
                .left()
        }

        val annullering = queries.totrinnskontroll.getOrError(tilsagn.id, TotrinnskontrollType.TILSAGN_ANNULLERING)
        return annullering.godkjenn(besluttetAv).mapLeft { it.toFieldErrors() }.map { godkjent ->
            queries.totrinnskontroll.upsert(godkjent)
            outbox.publish(godkjent)
            queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.ANNULLERT)
            logEndring("Tilsagn annullert", tilsagn.id, besluttetAv)
        }
    }

    private fun TransactionalQueryContext.avvisAnnullering(
        tilsagn: Tilsagn,
        besluttetAv: NavIdent,
        aarsaker: List<TilsagnStatusAarsak>,
        begrunnelse: String?,
    ): Either<List<FieldError>, Tilsagn> {
        if (tilsagn.status != TilsagnStatus.TIL_ANNULLERING) {
            return FieldError.of("Tilsagnet må ha status ${TilsagnStatus.TIL_ANNULLERING} for at annullering kan returneres")
                .nel()
                .left()
        }

        val annullering = queries.totrinnskontroll.getOrError(tilsagn.id, TotrinnskontrollType.TILSAGN_ANNULLERING)
        return annullering
            .returner(besluttetAv, begrunnelse, aarsaker.map { it.name })
            .mapLeft { it.toFieldErrors() }
            .map { returnert ->
                queries.totrinnskontroll.upsert(returnert)
                outbox.publish(returnert)
                queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.GODKJENT)

                val behandletAv = annullering.behandling.utfortAv
                if (behandletAv is NavIdent) {
                    sendNotifikasjonOmAvvistAnnullering(tilsagn, besluttetAv, behandletAv)
                }

                logEndring("Annullering avvist", tilsagn.id, besluttetAv)
            }
    }

    context(tx: TransactionalQueryContext)
    fun setTilOppgjor(
        tilsagn: Tilsagn,
        agent: Agent,
        aarsaker: List<String>,
        begrunnelse: String?,
        operation: String,
    ): Tilsagn = with(tx) {
        require(tilsagn.status == TilsagnStatus.GODKJENT) {
            "Kan bare gjøre opp godkjente tilsagn"
        }

        val oppgjor = Totrinnskontroll.opprett(
            id = UUID.randomUUID(),
            entityId = tilsagn.id,
            type = TotrinnskontrollType.TILSAGN_OPPGJOR,
            behandletAv = agent,
            behandletBegrunnelse = begrunnelse,
            behandletAarsaker = aarsaker,
        )
        queries.totrinnskontroll.upsert(oppgjor)
        outbox.publish(oppgjor)
        queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.TIL_OPPGJOR)

        return logEndring(operation, tilsagn.id, agent)
    }

    context(tx: TransactionalQueryContext)
    fun gjorOppTilsagn(
        tilsagn: Tilsagn,
        besluttetAv: Agent,
        operation: String,
    ): Either<List<FieldError>, Tilsagn> = with(tx) {
        if (tilsagn.status != TilsagnStatus.TIL_OPPGJOR) {
            return FieldError.of("Tilsagnet må ha status ${TilsagnStatus.TIL_OPPGJOR} for at oppgjør skal godkjennes")
                .nel()
                .left()
        }

        val oppgjor = queries.totrinnskontroll.getOrError(tilsagn.id, TotrinnskontrollType.TILSAGN_OPPGJOR)
        oppgjor.godkjenn(besluttetAv).mapLeft { it.toFieldErrors() }.map { godkjent ->
            queries.totrinnskontroll.upsert(godkjent)
            outbox.publish(godkjent)
            queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.OPPGJORT)
            logEndring(operation, tilsagn.id, besluttetAv)
        }
    }

    private fun TransactionalQueryContext.avvisOppgjor(
        tilsagn: Tilsagn,
        besluttetAv: NavIdent,
        aarsaker: List<TilsagnStatusAarsak>,
        begrunnelse: String?,
    ): Either<List<FieldError>, Tilsagn> {
        if (tilsagn.status != TilsagnStatus.TIL_OPPGJOR) {
            return FieldError.of("Tilsagnet må ha status ${TilsagnStatus.TIL_OPPGJOR} for at oppgjør kan returneres")
                .nel()
                .left()
        }

        val oppgjor = queries.totrinnskontroll.getOrError(tilsagn.id, TotrinnskontrollType.TILSAGN_OPPGJOR)
        return oppgjor
            .returner(besluttetAv, begrunnelse, aarsaker.map { it.name })
            .mapLeft { it.toFieldErrors() }
            .map { returnert ->
                queries.totrinnskontroll.upsert(returnert)
                outbox.publish(returnert)
                queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.GODKJENT)

                val behandletAv = oppgjor.behandling.utfortAv
                if (behandletAv is NavIdent) {
                    sendNotifikasjonOmAvvistOppgjor(tilsagn, besluttetAv, behandletAv)
                }

                logEndring("Oppgjør avvist", tilsagn.id, besluttetAv)
            }
    }

    private fun QueryContext.sendNotifikasjonOmAvvistAnnullering(
        tilsagn: Tilsagn,
        besluttetAv: NavIdent,
        behandletAv: NavIdent,
    ) {
        val beslutterNavn = getAnsattNavn(besluttetAv)
        val tilsagnDisplayName = tilsagn.type.displayName().lowercase()

        val notification = ScheduledNotification(
            title = "Et $tilsagnDisplayName du sendte til annullering er blitt avvist",
            description = listOf(
                "$beslutterNavn avviste annulleringen av $tilsagnDisplayName med kostnadssted ${tilsagn.kostnadssted.navn} for tiltaket ${tilsagn.getTiltaksnavn()}.",
                "Kontakt $beslutterNavn om dette er feil.",
            ).joinToString(" "),
            metadata = NotificationMetadata(
                linkText = "Gå til tilsagn",
                link = "/gjennomforinger/${tilsagn.gjennomforing.id}/tilsagn/${tilsagn.id}",
            ),
            createdAt = Instant.now(),
            targets = nonEmptyListOf(behandletAv),
        )
        queries.notifications.insert(notification)
    }

    private fun QueryContext.sendNotifikasjonOmAvvistOppgjor(
        tilsagn: Tilsagn,
        besluttetAv: NavIdent,
        behandletAv: NavIdent,
    ) {
        val beslutterNavn = getAnsattNavn(besluttetAv)
        val tilsagnDisplayName = tilsagn.type.displayName().lowercase()

        val notification = ScheduledNotification(
            title = "Et $tilsagnDisplayName du sendte til oppgjør er blitt avvist",
            description = listOf(
                "$beslutterNavn avviste oppgjøret av $tilsagnDisplayName med kostnadssted ${tilsagn.kostnadssted.navn} for tiltaket ${tilsagn.getTiltaksnavn()}.",
                "Kontakt $beslutterNavn om dette er feil.",
            ).joinToString(" "),
            metadata = NotificationMetadata(
                linkText = "Gå til tilsagn",
                link = "/gjennomforinger/${tilsagn.gjennomforing.id}/tilsagn/${tilsagn.id}",
            ),
            createdAt = Instant.now(),
            targets = nonEmptyListOf(behandletAv),
        )
        queries.notifications.insert(notification)
    }

    private fun QueryContext.sendNotifikasjonSlettetTilsagn(
        tilsagn: Tilsagn,
        besluttetAv: NavIdent,
        behandletAv: NavIdent,
    ) {
        val beslutterNavn = getAnsattNavn(besluttetAv)
        val tilsagnDisplayName = tilsagn.type.displayName().lowercase()

        val notification = ScheduledNotification(
            title = "Et $tilsagnDisplayName du sendte til godkjenning er blitt slettet",
            description = listOf(
                "$beslutterNavn slettet et $tilsagnDisplayName med kostnadssted ${tilsagn.kostnadssted.navn} for tiltaket ${tilsagn.getTiltaksnavn()}.",
                "Kontakt $beslutterNavn om dette er feil.",
            ).joinToString(" "),
            metadata = NotificationMetadata(
                linkText = "Gå til gjennomføringen",
                link = "/gjennomforinger/${tilsagn.gjennomforing.id}",
            ),
            targets = nonEmptyListOf(behandletAv),
            createdAt = Instant.now(),
        )
        queries.notifications.insert(notification)
    }

    private fun getAnsattNavn(navIdent: NavIdent): String {
        val beslutterAnsatt = navAnsattService.getNavAnsattByNavIdent(navIdent)
        return beslutterAnsatt?.fulltNavn() ?: navIdent.value
    }

    private fun TransactionalQueryContext.publishOpprettBestilling(tilsagn: Tilsagn) {
        val opprettelse = queries.totrinnskontroll.getOrError(tilsagn.id, TotrinnskontrollType.TILSAGN_OPPRETTELSE)
        val beslutning = checkNotNull(opprettelse.beslutning) {
            "Tilsagn id=${tilsagn.id} må være besluttet godkjent for å sendes til økonomi"
        }

        val gjennomforing = queries.gjennomforing.getGjennomforingTiltaksadministrasjon(tilsagn.gjennomforing.id)

        val avtale = when (gjennomforing) {
            is GjennomforingAvtale -> queries.avtale.getOrError(gjennomforing.avtaleId)
            is GjennomforingEnkeltplass -> null
        }

        val arrangor = when (val arrangor = repository.arrangor.get(gjennomforing.arrangor.id)) {
            is Arrangor.Utenlandsk -> {
                val utenlandskArrangor = requireNotNull(arrangor.adresse) {
                    "Mangler data om utenlandsk arrangør"
                }
                OpprettBestilling.Arrangor.Utenlandsk(
                    organisasjonsnummer = gjennomforing.arrangor.organisasjonsnummer,
                    navn = gjennomforing.arrangor.navn,
                    by = utenlandskArrangor.by,
                    postNummer = utenlandskArrangor.postNummer,
                    landKode = utenlandskArrangor.landKode,
                    gateNavn = utenlandskArrangor.gateNavn,
                )
            }

            is Arrangor.Norsk -> OpprettBestilling.Arrangor.Norsk(
                organisasjonsnummer = gjennomforing.arrangor.organisasjonsnummer,
            )
        }

        val bestilling = OpprettBestilling(
            bestillingsnummer = Bestillingsnummer(tilsagn.bestilling.bestillingsnummer),
            tilskuddstype = when (tilsagn.type) {
                TilsagnType.INVESTERING -> Tilskuddstype.TILTAK_INVESTERINGER
                else -> Tilskuddstype.TILTAK_DRIFTSTILSKUDD
            },
            tiltakskode = gjennomforing.tiltakstype.tiltakskode,
            arrangor = arrangor,
            kostnadssted = tilsagn.kostnadssted.enhetsnummer,
            avtalenummer = avtale?.sakarkivNummer?.value,
            belop = tilsagn.beregning.output.pris.belop,
            periode = tilsagn.periode,
            behandletAv = opprettelse.behandling.utfortAv.toOkonomiPart(),
            behandletTidspunkt = opprettelse.behandling.tidspunkt,
            besluttetAv = beslutning.utfortAv.toOkonomiPart(),
            besluttetTidspunkt = beslutning.tidspunkt,
            valuta = tilsagn.beregning.output.pris.valuta,
        )

        outbox.publish(OkonomiBestillingMelding.Bestilling(bestilling))
    }

    private fun TransactionalQueryContext.publishAnnullerBestilling(tilsagn: Tilsagn) {
        val annullering = queries.totrinnskontroll.getOrError(tilsagn.id, TotrinnskontrollType.TILSAGN_ANNULLERING)
        val beslutning = checkNotNull(annullering.beslutning) {
            "Tilsagn id=${tilsagn.id} må være besluttet annullert for å sendes som annullert til økonomi"
        }

        val annullerBestilling = AnnullerBestilling(
            bestillingsnummer = Bestillingsnummer(tilsagn.bestilling.bestillingsnummer),
            behandletAv = annullering.behandling.utfortAv.toOkonomiPart(),
            behandletTidspunkt = annullering.behandling.tidspunkt,
            besluttetAv = beslutning.utfortAv.toOkonomiPart(),
            besluttetTidspunkt = beslutning.tidspunkt,
        )

        outbox.publish(OkonomiBestillingMelding.Annullering(annullerBestilling))
    }

    private fun TransactionalQueryContext.publishGjorOppBestilling(tilsagn: Tilsagn) {
        val oppgjor = queries.totrinnskontroll.getOrError(tilsagn.id, TotrinnskontrollType.TILSAGN_OPPGJOR)
        val beslutning = checkNotNull(oppgjor.beslutning) {
            "Tilsagn id=${tilsagn.id} må være besluttet oppgjort for å kunne sendes til økonomi"
        }

        val faktura = GjorOppBestilling(
            bestillingsnummer = Bestillingsnummer(tilsagn.bestilling.bestillingsnummer),
            behandletAv = oppgjor.behandling.utfortAv.toOkonomiPart(),
            behandletTidspunkt = oppgjor.behandling.tidspunkt,
            besluttetAv = beslutning.utfortAv.toOkonomiPart(),
            besluttetTidspunkt = beslutning.tidspunkt,
        )

        outbox.publish(OkonomiBestillingMelding.GjorOppBestilling(faktura))
    }

    private fun QueryContext.logEndring(
        operation: String,
        tilsagnId: UUID,
        endretAv: Agent,
    ): Tilsagn {
        val tilsagn = queries.tilsagn.getOrError(tilsagnId)
        queries.endringshistorikk.logEndring(
            EndringshistorikkType.TILSAGN,
            operation,
            endretAv,
            tilsagnId,
            LocalDateTime.now(),
        ) {
            Json.encodeToJsonElement(tilsagn)
        }
        return tilsagn
    }

    private fun QueryContext.kanOppretteTilsagn(
        agent: Agent,
        gjennomforing: GjennomforingTiltaksadministrasjon,
    ): Boolean {
        val type = when (gjennomforing) {
            is GjennomforingAvtale -> GjennomforingType.AVTALE
            is GjennomforingEnkeltplass -> GjennomforingType.ENKELTPLASS
        }
        return when (agent) {
            Tiltaksadministrasjon -> true
            Arena, is no.nav.mulighetsrommet.model.Arrangor -> false
            is NavIdent -> OkonomiAuthorization.erSaksbehandler(queries.ansatt.getOrError(agent), type)
        }
    }

    private fun QueryContext.erSaksbehandler(navIdent: NavIdent, tilsagn: Tilsagn): Boolean {
        val ansatt = queries.ansatt.getOrError(navIdent)
        return OkonomiAuthorization.erSaksbehandler(ansatt, tilsagn.gjennomforing.type)
    }

    private fun QueryContext.kanBeslutteTilsagn(agent: Agent, tilsagn: Tilsagn): Boolean = when (agent) {
        Tiltaksadministrasjon -> true
        Arena, is no.nav.mulighetsrommet.model.Arrangor -> false
        is NavIdent -> erBeslutter(agent, tilsagn)
    }

    private fun QueryContext.erBeslutter(
        navIdent: NavIdent,
        tilsagn: Tilsagn,
    ): Boolean {
        val ansatt = queries.ansatt.getOrError(navIdent)
        val kontekst = OkonomiBeslutningContext(
            gjennomforingType = tilsagn.gjennomforing.type,
            kostnadssteder = nonEmptySetOf(tilsagn.kostnadssted.enhetsnummer),
        )
        return OkonomiAuthorization.erBeslutterTilsagn(ansatt, kontekst)
    }

    private fun QueryContext.updateFreeTextSearch(tilsagn: TilsagnDbo) {
        val fts = listOf(tilsagn.bestillingsnummer) +
            tilsagn.bestillingsnummer.replace("/", " ") +
            tilsagn.periode.toFreeTextSearch() +
            tilsagn.type.displayName()

        queries.tilsagn.setFreeTextSearch(tilsagn.id, fts)
    }
}
