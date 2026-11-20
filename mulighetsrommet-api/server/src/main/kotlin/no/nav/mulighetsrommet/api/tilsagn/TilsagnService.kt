package no.nav.mulighetsrommet.api.tilsagn

import arrow.core.Either
import arrow.core.left
import arrow.core.nel
import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import arrow.core.raise.context.Raise
import arrow.core.raise.context.bind
import arrow.core.raise.context.either
import arrow.core.raise.context.ensure
import arrow.core.raise.context.raise
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.admin.aarsakerbegrunnelse.AarsakerOgBegrunnelseRequest
import no.nav.mulighetsrommet.admin.endringshistorikk.EndringshistorikkType
import no.nav.mulighetsrommet.admin.navansatt.service.NavAnsattService
import no.nav.mulighetsrommet.admin.totrinnskontroll.toFieldErrors
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.TransactionalQueryContext
import no.nav.mulighetsrommet.api.domain.arrangor.Arrangor
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingAvtale
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingEnkeltplass
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingTiltaksadministrasjon
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingType
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsatt
import no.nav.mulighetsrommet.api.domain.navansatt.OkonomiAuthorization
import no.nav.mulighetsrommet.api.domain.navansatt.OkonomiAuthorizationError
import no.nav.mulighetsrommet.api.domain.navansatt.OkonomiBeslutningContext
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.Totrinnskontroll
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnDbo
import no.nav.mulighetsrommet.api.tilsagn.mapper.TilsagnOkonomiMapper
import no.nav.mulighetsrommet.api.tilsagn.model.BeregnTilsagnRequest
import no.nav.mulighetsrommet.api.tilsagn.model.GyldigTilsagnPeriode
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
import no.nav.mulighetsrommet.api.utbetaling.model.StengtPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingInputHelper
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingLinjeStatus
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.Arena
import no.nav.mulighetsrommet.model.NOK
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltaksadministrasjon
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.model.withValuta
import no.nav.mulighetsrommet.notifications.NotificationMetadata
import no.nav.mulighetsrommet.notifications.ScheduledNotification
import no.nav.mulighetsrommet.validation.FieldError
import no.nav.tiltak.okonomi.AnnullerBestilling
import no.nav.tiltak.okonomi.Bestillingsnummer
import no.nav.tiltak.okonomi.GjorOppBestilling
import no.nav.tiltak.okonomi.OkonomiBestillingMelding
import no.nav.tiltak.okonomi.OpprettBestilling
import no.nav.tiltak.okonomi.toOkonomiPart
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

class TilsagnService(
    private val gyldigTilsagnPeriode: GyldigTilsagnPeriode,
    private val db: ApiDatabase,
    private val navAnsattService: NavAnsattService,
) {
    fun upsert(request: TilsagnRequest, agent: Agent): Either<TilsagnError, Tilsagn> = db.transaction {
        upsertInTx(request, agent)
    }

    context(tx: TransactionalQueryContext)
    fun upsertInTx(request: TilsagnRequest, agent: Agent): Either<TilsagnError, Tilsagn> = with(tx) {
        either {
            requireNotNull(request.id) { "id mangler" }

            ensure(request.type != TilsagnType.TILSKUDD_TIL_OPPLAERING || agent == Tiltaksadministrasjon) {
                TilsagnError.Valideringsfeil(
                    FieldError.of("Tilskudd til opplæring kan ikke opprettes manuelt", TilsagnRequest::type).nel(),
                )
            }

            val gjennomforing = queries.gjennomforing.getGjennomforingTiltaksadministrasjon(request.gjennomforingId)
            kanOppretteTilsagn(agent, gjennomforing)

            val previous = queries.tilsagn.getAndAcquireLockOrNull(request.id)
            val stengt = when (gjennomforing) {
                is GjennomforingAvtale -> gjennomforing.stengt
                is GjennomforingEnkeltplass -> listOf()
            }
            val validated = TilsagnValidator
                .validate(
                    next = request,
                    previous = previous,
                    tiltakstypeNavn = gjennomforing.tiltakstype.navn,
                    arrangorSlettet = gjennomforing.arrangor.slettet,
                    gyldigTilsagnPeriode = gyldigTilsagnPeriode.forTiltakskode(gjennomforing.tiltakstype.tiltakskode),
                    gjennomforingSluttDato = gjennomforing.sluttDato,
                    prismodell = gjennomforing.prismodell,
                    stengt = stengt,
                )
                .mapLeft(TilsagnError::Valideringsfeil).bind()
            val lopenummer = previous?.lopenummer
                ?: queries.tilsagn.getNextLopenummeByGjennomforing(gjennomforing.id)

            val bestillingsnummer = previous?.bestilling?.bestillingsnummer
                ?: "A-${gjennomforing.lopenummer.value}-$lopenummer"

            val dbo = TilsagnDbo(
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
        either {
            val tilsagn = queries.tilsagn.getAndAcquireLock(id)
            kanSaksbehandle(navIdent, tilsagn)

            ensure(tilsagn.status == TilsagnStatus.RETURNERT) {
                TilsagnError.Valideringsfeil(FieldError.of("Kan ikke slette tilsagn som er godkjent").nel())
            }

            val opprettelse = queries.totrinnskontroll.getOrError(id, TotrinnskontrollType.TILSAGN_OPPRETTELSE)
            val behandletAv = opprettelse.behandling.utfortAv
            if (opprettelse.beslutning?.utfortAv == navIdent && behandletAv is NavIdent) {
                sendNotifikasjonSlettetTilsagn(tilsagn, besluttetAv = navIdent, behandletAv = behandletAv)
            }

            queries.tilsagn.delete(id)
        }
    }

    fun tilAnnulleringRequest(
        id: UUID,
        navIdent: NavIdent,
        request: AarsakerOgBegrunnelseRequest<TilsagnStatusAarsak>,
    ): Either<TilsagnError, Tilsagn> = db.transaction {
        either {
            val tilsagn = queries.tilsagn.getAndAcquireLock(id)
            kanSaksbehandle(navIdent, tilsagn)

            ensure(tilsagn.status == TilsagnStatus.GODKJENT) {
                TilsagnError.Valideringsfeil(FieldError.of("Kan bare annullere godkjente tilsagn").nel())
            }
            setTilAnnullering(tilsagn, navIdent, request.aarsaker.map { it.name }, request.begrunnelse)
        }
    }

    fun tilOppgjorRequest(
        id: UUID,
        navIdent: NavIdent,
        request: AarsakerOgBegrunnelseRequest<TilsagnStatusAarsak>,
    ): Either<TilsagnError, Tilsagn> = db.transaction {
        either {
            val tilsagn = queries.tilsagn.getAndAcquireLock(id)
            kanSaksbehandle(navIdent, tilsagn)

            ensure(tilsagn.status == TilsagnStatus.GODKJENT) {
                TilsagnError.Valideringsfeil(FieldError.of("Kan bare gjøre opp godkjente tilsagn").nel())
            }
            setTilOppgjor(
                tilsagn,
                navIdent,
                aarsaker = request.aarsaker.map { it.name },
                begrunnelse = request.begrunnelse,
                operation = "Sendt til oppgjør",
            )
        }
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
                            pris = ValutaBelop(
                                request.beregning.pris?.belop ?: 0,
                                request.beregning.pris?.valuta ?: Valuta.NOK,
                            ),
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
        either {
            val tilsagn = queries.tilsagn.getAndAcquireLock(id)

            kanBeslutteTilsagn(agent, tilsagn).bind()

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
                .bind()
        }
    }

    fun returnerTilsagn(
        id: UUID,
        navIdent: NavIdent,
        aarsaker: List<TilsagnStatusAarsak>,
        begrunnelse: String?,
    ): Either<TilsagnError, Tilsagn> = db.transaction {
        either {
            val tilsagn = queries.tilsagn.getAndAcquireLock(id)

            kanReturnereTilsagn(navIdent, tilsagn).bind()

            when (tilsagn.status) {
                TilsagnStatus.OPPGJORT, TilsagnStatus.ANNULLERT, TilsagnStatus.GODKJENT, TilsagnStatus.RETURNERT,
                -> FieldError.of("Tilsagnet kan ikke returneres fordi det har status ${tilsagn.status.beskrivelse}")
                    .nel()
                    .left()

                TilsagnStatus.TIL_GODKJENNING -> returnerTilsagn(tilsagn, navIdent, aarsaker, begrunnelse)

                TilsagnStatus.TIL_ANNULLERING -> avvisAnnullering(tilsagn, navIdent, aarsaker, begrunnelse)

                TilsagnStatus.TIL_OPPGJOR -> avvisOppgjor(tilsagn, navIdent, aarsaker, begrunnelse)
            }.mapLeft(TilsagnError::Valideringsfeil).bind()
        }
    }

    private fun QueryContext.kanReturnereTilsagn(
        navIdent: NavIdent,
        tilsagn: Tilsagn,
    ): Either<TilsagnError, Unit> = either {
        val kanBehandleTilsagn = either { kanSaksbehandle(navIdent, tilsagn) }
        val kanBeslutteTilsagn = either { erBeslutter(navIdent, tilsagn) }
        if (kanBehandleTilsagn.isLeft() && kanBeslutteTilsagn.isLeft()) {
            val manglendeRoller = buildSet {
                kanBehandleTilsagn.onLeft { error ->
                    if (error is TilsagnError.ManglerTilgang) {
                        addAll(error.roller)
                    }
                }
                kanBeslutteTilsagn.onLeft { error ->
                    if (error is TilsagnError.ManglerTilgang) {
                        addAll(error.roller)
                    }
                }
            }

            raise(
                TilsagnError.ManglerTilgang(
                    "Du kan ikke returnere tilsagnet fordi du mangler tilgang",
                    navIdent,
                    manglendeRoller,
                ),
            )
        }
    }

    fun republishOpprettBestilling(bestillingsnummer: String): Tilsagn = db.transaction {
        val tilsagn = queries.tilsagn.getOrError(bestillingsnummer)
        publishOpprettBestilling(tilsagn)
        tilsagn
    }

    private fun TransactionalQueryContext.godkjennTilsagn(
        tilsagn: Tilsagn,
        besluttetAv: Agent,
    ): Either<List<FieldError>, Tilsagn> = either {
        ensure(tilsagn.status == TilsagnStatus.TIL_GODKJENNING) {
            FieldError.of("Tilsagnet må ha status ${TilsagnStatus.TIL_GODKJENNING} for å godkjennes")
                .nel()
        }

        val opprettelse = queries.totrinnskontroll.getOrError(tilsagn.id, TotrinnskontrollType.TILSAGN_OPPRETTELSE)
        val godkjent = opprettelse.godkjenn(besluttetAv).mapLeft { it.toFieldErrors() }.bind()

        queries.totrinnskontroll.upsert(godkjent)
        outbox.publish(godkjent)
        queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.GODKJENT)
        logEndring("Tilsagn godkjent", tilsagn.id, besluttetAv)
    }

    private fun TransactionalQueryContext.returnerTilsagn(
        tilsagn: Tilsagn,
        besluttetAv: NavIdent,
        aarsaker: List<TilsagnStatusAarsak>,
        begrunnelse: String?,
    ): Either<List<FieldError>, Tilsagn> = either {
        ensure(tilsagn.status == TilsagnStatus.TIL_GODKJENNING) {
            FieldError.of("Tilsagnet må ha status ${TilsagnStatus.TIL_GODKJENNING} for å returneres").nel()
        }

        ensure(aarsaker.isNotEmpty()) {
            FieldError.of("Årsaker er påkrevd").nel()
        }

        val opprettelse = queries.totrinnskontroll.getOrError(tilsagn.id, TotrinnskontrollType.TILSAGN_OPPRETTELSE)
        val returnert = opprettelse
            .returner(besluttetAv, begrunnelse, aarsaker.map { it.name })
            .mapLeft { it.toFieldErrors() }.bind()

        queries.totrinnskontroll.upsert(returnert)
        outbox.publish(returnert)
        queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.RETURNERT)
        logEndring("Tilsagn returnert", tilsagn.id, besluttetAv)
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
    ): Either<List<FieldError>, Tilsagn> = either {
        ensure(tilsagn.status == TilsagnStatus.TIL_ANNULLERING) {
            FieldError.of("Tilsagnet må ha status ${TilsagnStatus.TIL_ANNULLERING} for at annullering skal godkjennes")
                .nel()
        }
        val linjer = db.session { queries.utbetalingLinje.getByTilsagnId(tilsagn.id) }
            .filter { it.status != UtbetalingLinjeStatus.AVBRUTT }

        ensure(linjer.isEmpty()) {
            FieldError.of("Tilsagnet kan ikke annulleres fordi det har blitt brukt i utbetalinger")
                .nel()
        }

        val annullering = queries.totrinnskontroll.getOrError(tilsagn.id, TotrinnskontrollType.TILSAGN_ANNULLERING)
        val godkjent = annullering.godkjenn(besluttetAv).mapLeft { it.toFieldErrors() }.bind()
        queries.totrinnskontroll.upsert(godkjent)
        outbox.publish(godkjent)
        queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.ANNULLERT)
        logEndring("Tilsagn annullert", tilsagn.id, besluttetAv)
    }

    private fun TransactionalQueryContext.avvisAnnullering(
        tilsagn: Tilsagn,
        besluttetAv: NavIdent,
        aarsaker: List<TilsagnStatusAarsak>,
        begrunnelse: String?,
    ): Either<List<FieldError>, Tilsagn> = either {
        ensure(tilsagn.status == TilsagnStatus.TIL_ANNULLERING) {
            FieldError.of("Tilsagnet må ha status ${TilsagnStatus.TIL_ANNULLERING} for at annullering kan returneres")
                .nel()
        }

        val annullering = queries.totrinnskontroll.getOrError(tilsagn.id, TotrinnskontrollType.TILSAGN_ANNULLERING)
        val returnert = annullering
            .returner(besluttetAv, begrunnelse, aarsaker.map { it.name })
            .mapLeft { it.toFieldErrors() }.bind()

        queries.totrinnskontroll.upsert(returnert)
        outbox.publish(returnert)
        queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.GODKJENT)

        val behandletAv = annullering.behandling.utfortAv
        if (behandletAv is NavIdent) {
            sendNotifikasjonOmAvvistAnnullering(tilsagn, besluttetAv, behandletAv)
        }

        logEndring("Annullering avvist", tilsagn.id, besluttetAv)
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
        either {
            ensure(tilsagn.status == TilsagnStatus.TIL_OPPGJOR) {
                FieldError.of("Tilsagnet må ha status ${TilsagnStatus.TIL_OPPGJOR} for at oppgjør skal godkjennes")
                    .nel()
            }

            val oppgjor = queries.totrinnskontroll.getOrError(tilsagn.id, TotrinnskontrollType.TILSAGN_OPPGJOR)
            val godkjent = oppgjor.godkjenn(besluttetAv).mapLeft { it.toFieldErrors() }.bind()
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
    ): Either<List<FieldError>, Tilsagn> = either {
        ensure(tilsagn.status == TilsagnStatus.TIL_OPPGJOR) {
            FieldError.of("Tilsagnet må ha status ${TilsagnStatus.TIL_OPPGJOR} for at oppgjør kan returneres")
                .nel()
        }

        val oppgjor = queries.totrinnskontroll.getOrError(tilsagn.id, TotrinnskontrollType.TILSAGN_OPPGJOR)
        val returnert = oppgjor
            .returner(besluttetAv, begrunnelse, aarsaker.map { it.name })
            .mapLeft { it.toFieldErrors() }.bind()

        queries.totrinnskontroll.upsert(returnert)
        outbox.publish(returnert)
        queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.GODKJENT)

        val behandletAv = oppgjor.behandling.utfortAv
        if (behandletAv is NavIdent) {
            sendNotifikasjonOmAvvistOppgjor(tilsagn, besluttetAv, behandletAv)
        }

        logEndring("Oppgjør avvist", tilsagn.id, besluttetAv)
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
            regnskapskonto = TilsagnOkonomiMapper.toRegnskapskonto(tilsagn.type),
            artskonto = when (gjennomforing) {
                is GjennomforingAvtale,
                -> TilsagnOkonomiMapper.toArtskontoAvtale(gjennomforing.tiltakstype.tiltakskode)

                is GjennomforingEnkeltplass,
                -> TilsagnOkonomiMapper.toArtskontoEnkeltplass(gjennomforing.tiltakstype.tiltakskode)
            },
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

    context(errors: Raise<TilsagnError>)
    private fun QueryContext.kanOppretteTilsagn(
        agent: Agent,
        gjennomforing: GjennomforingTiltaksadministrasjon,
    ) {
        val type = when (gjennomforing) {
            is GjennomforingAvtale -> GjennomforingType.AVTALE
            is GjennomforingEnkeltplass -> GjennomforingType.ENKELTPLASS
        }
        when (agent) {
            Tiltaksadministrasjon -> Unit

            Arena, is no.nav.mulighetsrommet.model.Arrangor -> errors.raise(TilsagnError.AgentUgyldigHandling("$agent kan ikke opprette tilsagn"))

            is NavIdent -> {
                val ansatt = queries.ansatt.getOrError(agent)
                OkonomiAuthorization.erSaksbehandler(ansatt, type).onLeft {
                    errors.raise(it.toTilsagnError(ansatt.navIdent))
                }
            }
        }
    }

    context(errors: Raise<TilsagnError>)
    private fun QueryContext.kanSaksbehandle(navIdent: NavIdent, tilsagn: Tilsagn) {
        val ansatt = queries.ansatt.getOrError(navIdent)
        errors.kanSaksbehandle(ansatt, tilsagn.gjennomforing.type)
    }

    private fun QueryContext.kanBeslutteTilsagn(agent: Agent, tilsagn: Tilsagn): Either<TilsagnError, Unit> = either {
        when (agent) {
            Tiltaksadministrasjon -> Unit
            Arena, is no.nav.mulighetsrommet.model.Arrangor -> raise(TilsagnError.AgentUgyldigHandling("$agent kan ikke beslutte tilsagn"))
            is NavIdent -> erBeslutter(agent, tilsagn)
        }
    }

    context(errors: Raise<TilsagnError>)
    private fun QueryContext.erBeslutter(
        navIdent: NavIdent,
        tilsagn: Tilsagn,
    ) {
        val ansatt = queries.ansatt.getOrError(navIdent)
        errors.kanBeslutteTilsagn(ansatt, tilsagn)
    }

    private fun QueryContext.updateFreeTextSearch(tilsagn: TilsagnDbo) {
        val fts = buildList {
            addAll(listOf(tilsagn.bestillingsnummer))
            add(tilsagn.bestillingsnummer.replace("/", " "))
            addAll(tilsagn.periode.toFreeTextSearch())
            add(tilsagn.type.displayName())
        }

        queries.tilsagn.setFreeTextSearch(tilsagn.id, fts)
    }

    private fun Raise<TilsagnError>.kanSaksbehandle(ansatt: NavAnsatt, gjennomforingType: GjennomforingType) {
        OkonomiAuthorization.erSaksbehandler(ansatt, gjennomforingType).onLeft {
            raise(it.toTilsagnError(ansatt.navIdent))
        }
    }

    private fun Raise<TilsagnError>.kanBeslutteTilsagn(ansatt: NavAnsatt, tilsagn: Tilsagn) {
        val kontekst = OkonomiBeslutningContext(
            gjennomforingType = tilsagn.gjennomforing.type,
            kostnadssteder = nonEmptySetOf(tilsagn.kostnadssted.enhetsnummer),
        )

        OkonomiAuthorization.erBeslutterTilsagn(ansatt, kontekst).onLeft {
            raise(it.toTilsagnError(ansatt.navIdent))
        }
    }
}

fun OkonomiAuthorizationError.toTilsagnError(navIdent: NavIdent): TilsagnError = when (this) {
    is OkonomiAuthorizationError.ManglerTilgang -> TilsagnError.ManglerTilgang("Du mangler tilgang til tilsagnet", navIdent, requiredRoles)
    is OkonomiAuthorizationError.ManglerKontorspesifikkTilgang -> TilsagnError.ManglerTilgang("Du mangler kontorspesifikk tilgang til tilsagnet", navIdent, requiredRoles)
}
