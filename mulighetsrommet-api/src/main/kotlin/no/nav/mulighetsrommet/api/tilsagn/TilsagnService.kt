package no.nav.mulighetsrommet.api.tilsagn

import arrow.core.Either
import arrow.core.left
import arrow.core.nel
import arrow.core.nonEmptyListOf
import arrow.core.right
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.TransactionalQueryContext
import no.nav.mulighetsrommet.api.aarsakerforklaring.AarsakerOgForklaringRequest
import no.nav.mulighetsrommet.api.avtale.mapper.satser
import no.nav.mulighetsrommet.api.avtale.model.findAvtaltSats
import no.nav.mulighetsrommet.api.endringshistorikk.EndringshistorikkType
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtale
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingEnkeltplass
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsatt
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.navansatt.service.NavAnsattService
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnHandling
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnDbo
import no.nav.mulighetsrommet.api.tilsagn.model.BeregnTilsagnRequest
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregning
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFastSatsPerTiltaksplassPerManed
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFri
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningPrisPerHeleUkesverk
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningPrisPerManedsverk
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningPrisPerUkesverk
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningType
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnRequest
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatusAarsak
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.api.totrinnskontroll.TotrinnskontrollService
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollType
import no.nav.mulighetsrommet.api.utbetaling.service.erBeslutter
import no.nav.mulighetsrommet.api.utbetaling.service.erSaksbehandler
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.Arena
import no.nav.mulighetsrommet.model.Arrangor
import no.nav.mulighetsrommet.model.NOK
import no.nav.mulighetsrommet.model.NavEnhetNummer
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
    private val totrinnskontroll: TotrinnskontrollService,
) {
    data class Config(
        val bestillingTopic: String,
        val gyldigTilsagnPeriode: Map<Tiltakskode, Periode>,
    )

    fun upsert(request: TilsagnRequest, agent: Agent): Either<List<FieldError>, Tilsagn> = db.transaction {
        upsertInTx(request, agent)
    }

    context(tx: TransactionalQueryContext)
    fun upsertInTx(request: TilsagnRequest, agent: Agent): Either<List<FieldError>, Tilsagn> = with(tx) {
        requireNotNull(request.id) { "id mangler" }

        val gjennomforing = queries.gjennomforing.getGjennomforingTiltaksadministrasjon(request.gjennomforingId)

        val previous = queries.tilsagn.get(request.id)
        return TilsagnValidator
            .validate(
                next = request,
                previous = previous,
                tiltakstypeNavn = gjennomforing.tiltakstype.navn,
                arrangorSlettet = gjennomforing.arrangor.slettet,
                gyldigTilsagnPeriode = config.gyldigTilsagnPeriode[gjennomforing.tiltakstype.tiltakskode],
                gjennomforingSluttDato = gjennomforing.sluttDato,
                prismodell = gjennomforing.prismodell,
            )
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
                totrinnskontroll.opprett(request.id, TotrinnskontrollType.TILSAGN_OPPRETTELSE, agent)
                logEndring("Sendt til godkjenning", dbo.id, agent).also {
                    updateFreeTextSearch(dbo)
                }
            }
    }

    fun slettTilsagn(id: UUID, navIdent: NavIdent): Either<List<FieldError>, Unit> = db.transaction {
        val tilsagn = queries.tilsagn.getAndAquireLock(id)
        if (tilsagn.status != TilsagnStatus.RETURNERT) {
            return FieldError.of("Kan ikke slette tilsagn som er godkjent").nel().left()
        }

        val opprettelse = totrinnskontroll.getOrError(id, TotrinnskontrollType.TILSAGN_OPPRETTELSE)
        if (opprettelse.besluttetAv == navIdent && opprettelse.behandletAv is NavIdent) {
            sendNotifikasjonSlettetTilsagn(tilsagn, besluttetAv = navIdent, behandletAv = opprettelse.behandletAv)
        }

        queries.tilsagn.delete(id)

        Unit.right()
    }

    fun tilAnnulleringRequest(
        id: UUID,
        navIdent: NavIdent,
        request: AarsakerOgForklaringRequest<TilsagnStatusAarsak>,
    ): Tilsagn = db.transaction {
        val tilsagn = queries.tilsagn.getAndAquireLock(id)
        setTilAnnullering(tilsagn, navIdent, request.aarsaker.map { it.name }, request.forklaring)
    }

    fun tilOppgjorRequest(
        id: UUID,
        navIdent: NavIdent,
        request: AarsakerOgForklaringRequest<TilsagnStatusAarsak>,
    ): Tilsagn = db.transaction {
        val tilsagn = queries.tilsagn.getAndAquireLock(id)
        setTilOppgjor(
            tilsagn,
            navIdent,
            aarsaker = request.aarsaker.map { it.name },
            forklaring = request.forklaring,
            operation = "Sendt til oppgjør",
        )
    }

    fun beregnTilsagnUnvalidated(request: BeregnTilsagnRequest): TilsagnBeregning? = db.session {
        return try {
            when (request.beregning.type) {
                TilsagnBeregningType.FRI ->
                    TilsagnBeregningFri.beregn(
                        TilsagnBeregningFri.Input(
                            linjer = request.beregning.linjer.orEmpty().map {
                                TilsagnBeregningFri.InputLinje(
                                    id = it.id,
                                    beskrivelse = it.beskrivelse ?: "",
                                    pris = it.pris ?: 0.NOK,
                                    antall = it.antall ?: 0,
                                )
                            },
                            prisbetingelser = request.beregning.prisbetingelser,
                        ),
                    )

                TilsagnBeregningType.FAST_SATS_PER_TILTAKSPLASS_PER_MANED ->
                    beregnTilsagnFallbackResolver(request)?.let { fallback ->
                        TilsagnBeregningFastSatsPerTiltaksplassPerManed.beregn(
                            TilsagnBeregningFastSatsPerTiltaksplassPerManed.Input(
                                periode = fallback.periode,
                                sats = fallback.sats,
                                antallPlasser = fallback.antallPlasser,
                            ),
                        )
                    }

                TilsagnBeregningType.PRIS_PER_MANEDSVERK ->
                    beregnTilsagnFallbackResolver(request)?.let { fallback ->
                        TilsagnBeregningPrisPerManedsverk.beregn(
                            TilsagnBeregningPrisPerManedsverk.Input(
                                periode = fallback.periode,
                                sats = fallback.sats,
                                antallPlasser = fallback.antallPlasser,
                                prisbetingelser = fallback.prisbetingelser,
                            ),
                        )
                    }

                TilsagnBeregningType.PRIS_PER_UKESVERK ->
                    beregnTilsagnFallbackResolver(request)?.let { fallback ->
                        TilsagnBeregningPrisPerUkesverk.beregn(
                            TilsagnBeregningPrisPerUkesverk.Input(
                                periode = fallback.periode,
                                sats = fallback.sats,
                                antallPlasser = fallback.antallPlasser,
                                prisbetingelser = fallback.prisbetingelser,
                            ),
                        )
                    }

                TilsagnBeregningType.PRIS_PER_HELE_UKESVERK ->
                    beregnTilsagnFallbackResolver(request)?.let { fallback ->
                        TilsagnBeregningPrisPerHeleUkesverk.beregn(
                            TilsagnBeregningPrisPerHeleUkesverk.Input(
                                periode = fallback.periode,
                                sats = fallback.sats,
                                antallPlasser = fallback.antallPlasser,
                                prisbetingelser = fallback.prisbetingelser,
                            ),
                        )
                    }

                TilsagnBeregningType.PRIS_PER_TIME_OPPFOLGING ->
                    beregnTilsagnFallbackResolver(request)?.let { fallback ->
                        TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker.beregn(
                            TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker.Input(
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
    )

    private fun beregnTilsagnFallbackResolver(request: BeregnTilsagnRequest): TilsagnBeregningFallbackResolver? = db.session {
        if (request.periodeStart == null || request.periodeSlutt == null || !request.periodeStart.isBefore(request.periodeSlutt)) {
            return null
        }

        val prismodell = queries.gjennomforing.getPrismodellOrError(request.gjennomforingId)
        val avtaltSats = prismodell.satser().findAvtaltSats(request.periodeStart)

        val antallPlasserFallback = request.beregning.antallPlasser ?: 0
        val antallTimerOppfolgingPerDeltakerFallback = request.beregning.antallTimerOppfolgingPerDeltaker ?: 0
        val periode = Periode.fromInclusiveDates(request.periodeStart, request.periodeSlutt)

        return TilsagnBeregningFallbackResolver(
            sats = avtaltSats?.sats ?: ValutaBelop(0, Valuta.NOK),
            periode = periode,
            antallPlasser = antallPlasserFallback,
            antallTimerOppfolgingPerDeltaker = antallTimerOppfolgingPerDeltakerFallback,
            prisbetingelser = request.beregning.prisbetingelser,
        )
    }

    fun godkjennTilsagn(
        id: UUID,
        agent: Agent,
    ): Either<List<FieldError>, Tilsagn> = db.transaction { godkjennTilsagnInTx(id, agent) }

    context(tx: TransactionalQueryContext)
    fun godkjennTilsagnInTx(
        id: UUID,
        agent: Agent,
    ): Either<List<FieldError>, Tilsagn> = with(tx) {
        val tilsagn = queries.tilsagn.getAndAquireLock(id)

        when (agent) {
            Tiltaksadministrasjon -> Unit

            Arena,
            Arrangor,
            -> return FieldError.of("$agent kan ikke beslutte tilsagn").nel().left()

            is NavIdent -> {
                val ansatt = queries.ansatt.getByNavIdentOrError(agent)
                if (!erBeslutter(ansatt, tilsagn.kostnadssted)) {
                    return FieldError.of("Du kan ikke beslutte tilsagnet fordi du mangler budsjettmyndighet ved tilsagnets kostnadssted (${tilsagn.kostnadssted.navn})")
                        .nel()
                        .left()
                }
            }
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
        }
    }

    fun returnerTilsagn(
        id: UUID,
        navIdent: NavIdent,
        aarsaker: List<TilsagnStatusAarsak>,
        forklaring: String?,
    ): Either<List<FieldError>, Tilsagn> = db.transaction {
        val tilsagn = queries.tilsagn.getAndAquireLock(id)

        val ansatt = queries.ansatt.getByNavIdentOrError(navIdent)
        if (!(erSaksbehandler(ansatt) || erBeslutter(ansatt, tilsagn.kostnadssted))) {
            return FieldError.of("Du kan ikke returnere tilsagnet fordi du mangler tilgang").nel().left()
        }

        when (tilsagn.status) {
            TilsagnStatus.OPPGJORT, TilsagnStatus.ANNULLERT, TilsagnStatus.GODKJENT, TilsagnStatus.RETURNERT,
            -> FieldError.of("Tilsagnet kan ikke returneres fordi det har status ${tilsagn.status.beskrivelse}")
                .nel()
                .left()

            TilsagnStatus.TIL_GODKJENNING -> returnerTilsagn(tilsagn, navIdent, aarsaker, forklaring)

            TilsagnStatus.TIL_ANNULLERING -> avvisAnnullering(tilsagn, navIdent, aarsaker, forklaring)

            TilsagnStatus.TIL_OPPGJOR -> avvisOppgjor(tilsagn, navIdent, aarsaker, forklaring)
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
    ): Either<List<FieldError>, Tilsagn> {
        if (tilsagn.status != TilsagnStatus.TIL_GODKJENNING) {
            return FieldError.of("Tilsagnet må ha status ${TilsagnStatus.TIL_GODKJENNING} for å godkjennes")
                .nel()
                .left()
        }

        val opprettelse = totrinnskontroll.getOrError(tilsagn.id, TotrinnskontrollType.TILSAGN_OPPRETTELSE)
        return totrinnskontroll.godkjent(opprettelse, besluttetAv).map {
            queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.GODKJENT)
            logEndring("Tilsagn godkjent", tilsagn.id, besluttetAv)
        }
    }

    private fun TransactionalQueryContext.returnerTilsagn(
        tilsagn: Tilsagn,
        besluttetAv: NavIdent,
        aarsaker: List<TilsagnStatusAarsak>,
        forklaring: String?,
    ): Either<List<FieldError>, Tilsagn> {
        if (tilsagn.status != TilsagnStatus.TIL_GODKJENNING) {
            return FieldError.of("Tilsagnet må ha status ${TilsagnStatus.TIL_GODKJENNING} for å returneres")
                .nel()
                .left()
        }

        if (aarsaker.isEmpty()) {
            return FieldError.of("Årsaker er påkrevd").nel().left()
        }

        val opprettelse = totrinnskontroll.getOrError(tilsagn.id, TotrinnskontrollType.TILSAGN_OPPRETTELSE)
        return totrinnskontroll.avvist(opprettelse, besluttetAv, aarsaker.map { it.name }, forklaring).map {
            queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.RETURNERT)
            logEndring("Tilsagn returnert", tilsagn.id, besluttetAv)
        }
    }

    private fun TransactionalQueryContext.setTilAnnullering(
        tilsagn: Tilsagn,
        behandletAv: Agent,
        aarsaker: List<String>,
        forklaring: String?,
    ): Tilsagn {
        require(tilsagn.status == TilsagnStatus.GODKJENT) {
            "Kan bare annullere godkjente tilsagn"
        }

        totrinnskontroll.opprett(
            tilsagn.id,
            TotrinnskontrollType.TILSAGN_ANNULLERING,
            behandletAv,
            aarsaker,
            forklaring,
        )
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

        val annullering = totrinnskontroll.getOrError(tilsagn.id, TotrinnskontrollType.TILSAGN_ANNULLERING)
        return totrinnskontroll.godkjent(annullering, besluttetAv).map {
            queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.ANNULLERT)
            logEndring("Tilsagn annullert", tilsagn.id, besluttetAv)
        }
    }

    private fun TransactionalQueryContext.avvisAnnullering(
        tilsagn: Tilsagn,
        besluttetAv: NavIdent,
        aarsaker: List<TilsagnStatusAarsak>,
        forklaring: String?,
    ): Either<List<FieldError>, Tilsagn> {
        if (tilsagn.status != TilsagnStatus.TIL_ANNULLERING) {
            return FieldError.of("Tilsagnet må ha status ${TilsagnStatus.TIL_ANNULLERING} for at annullering skal avvises")
                .nel()
                .left()
        }

        val annullering = totrinnskontroll.getOrError(tilsagn.id, TotrinnskontrollType.TILSAGN_ANNULLERING)
        return totrinnskontroll.avvist(annullering, besluttetAv, aarsaker.map { it.name }, forklaring).map {
            queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.GODKJENT)

            if (annullering.behandletAv is NavIdent) {
                sendNotifikasjonOmAvvistAnnullering(tilsagn, besluttetAv, annullering.behandletAv)
            }

            logEndring("Annullering avvist", tilsagn.id, besluttetAv)
        }
    }

    context(tx: TransactionalQueryContext)
    fun setTilOppgjor(
        tilsagn: Tilsagn,
        agent: Agent,
        aarsaker: List<String>,
        forklaring: String?,
        operation: String,
    ): Tilsagn = with(tx) {
        require(tilsagn.status == TilsagnStatus.GODKJENT) {
            "Kan bare gjøre opp godkjente tilsagn"
        }

        totrinnskontroll.opprett(
            tilsagn.id,
            TotrinnskontrollType.TILSAGN_OPPGJOR,
            agent,
            aarsaker,
            forklaring,
        )
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

        val oppgjor = totrinnskontroll.getOrError(tilsagn.id, TotrinnskontrollType.TILSAGN_OPPGJOR)
        totrinnskontroll.godkjent(oppgjor, besluttetAv).map {
            queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.OPPGJORT)
            logEndring(operation, tilsagn.id, besluttetAv)
        }
    }

    private fun TransactionalQueryContext.avvisOppgjor(
        tilsagn: Tilsagn,
        besluttetAv: NavIdent,
        aarsaker: List<TilsagnStatusAarsak>,
        forklaring: String?,
    ): Either<List<FieldError>, Tilsagn> {
        if (tilsagn.status != TilsagnStatus.TIL_OPPGJOR) {
            return FieldError.of("Tilsagnet må ha status ${TilsagnStatus.TIL_OPPGJOR} for at oppgjør skal avvises")
                .nel()
                .left()
        }

        val oppgjor = totrinnskontroll.getOrError(tilsagn.id, TotrinnskontrollType.TILSAGN_OPPGJOR)
        return totrinnskontroll.avvist(oppgjor, besluttetAv, aarsaker.map { it.name }, forklaring).map {
            queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.GODKJENT)

            if (oppgjor.behandletAv is NavIdent) {
                sendNotifikasjonOmAvvistOppgjor(tilsagn, besluttetAv, oppgjor.behandletAv)
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
        return beslutterAnsatt?.displayName() ?: navIdent.value
    }

    private fun TransactionalQueryContext.publishOpprettBestilling(tilsagn: Tilsagn) {
        val opprettelse = totrinnskontroll.getOrError(tilsagn.id, TotrinnskontrollType.TILSAGN_OPPRETTELSE)
        check(opprettelse.besluttetAv != null && opprettelse.besluttetTidspunkt != null) {
            "Tilsagn id=${tilsagn.id} må være besluttet godkjent for å sendes til økonomi"
        }

        val gjennomforing = queries.gjennomforing.getGjennomforingTiltaksadministrasjon(tilsagn.gjennomforing.id)

        val avtale = when (gjennomforing) {
            is GjennomforingAvtale -> queries.avtale.getOrError(gjennomforing.avtaleId)
            is GjennomforingEnkeltplass -> null
        }

        val arrangorErUtenlandsk = queries.arrangor.getById(gjennomforing.arrangor.id).erUtenlandsk
        val arrangor = if (arrangorErUtenlandsk) {
            val utenlandskArrangor = requireNotNull(queries.arrangor.getUtenlandskArrangor(gjennomforing.arrangor.id)) {
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
        } else {
            OpprettBestilling.Arrangor.Norsk(
                organisasjonsnummer = gjennomforing.arrangor.organisasjonsnummer,
            )
        }

        val bestilling = OpprettBestilling(
            bestillingsnummer = tilsagn.bestilling.bestillingsnummer,
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
            behandletAv = opprettelse.behandletAv.toOkonomiPart(),
            behandletTidspunkt = opprettelse.behandletTidspunkt,
            besluttetAv = opprettelse.besluttetAv.toOkonomiPart(),
            besluttetTidspunkt = opprettelse.besluttetTidspunkt,
            valuta = tilsagn.beregning.output.pris.valuta,
        )

        storeOkonomiMelding(bestilling.bestillingsnummer, OkonomiBestillingMelding.Bestilling(bestilling))
    }

    private fun TransactionalQueryContext.publishAnnullerBestilling(tilsagn: Tilsagn) {
        val annullering = totrinnskontroll.getOrError(tilsagn.id, TotrinnskontrollType.TILSAGN_ANNULLERING)
        check(annullering.besluttetAv != null && annullering.besluttetTidspunkt != null) {
            "Tilsagn id=${tilsagn.id} må være besluttet annullert for å sendes som annullert til økonomi"
        }

        val annullerBestilling = AnnullerBestilling(
            bestillingsnummer = tilsagn.bestilling.bestillingsnummer,
            behandletAv = annullering.behandletAv.toOkonomiPart(),
            behandletTidspunkt = annullering.behandletTidspunkt,
            besluttetAv = annullering.besluttetAv.toOkonomiPart(),
            besluttetTidspunkt = annullering.besluttetTidspunkt,
        )

        storeOkonomiMelding(
            tilsagn.bestilling.bestillingsnummer,
            OkonomiBestillingMelding.Annullering(annullerBestilling),
        )
    }

    private fun TransactionalQueryContext.publishGjorOppBestilling(tilsagn: Tilsagn) {
        val oppgjor = totrinnskontroll.getOrError(tilsagn.id, TotrinnskontrollType.TILSAGN_OPPGJOR)
        check(oppgjor.besluttetAv != null && oppgjor.besluttetTidspunkt != null) {
            "Tilsagn id=${tilsagn.id} må være besluttet oppgjort for å kunne sendes til økonomi"
        }

        val faktura = GjorOppBestilling(
            bestillingsnummer = tilsagn.bestilling.bestillingsnummer,
            behandletAv = oppgjor.behandletAv.toOkonomiPart(),
            behandletTidspunkt = oppgjor.behandletTidspunkt,
            besluttetAv = oppgjor.besluttetAv.toOkonomiPart(),
            besluttetTidspunkt = oppgjor.besluttetTidspunkt,
        )

        storeOkonomiMelding(
            tilsagn.bestilling.bestillingsnummer,
            OkonomiBestillingMelding.GjorOppBestilling(faktura),
        )
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

    private fun TransactionalQueryContext.storeOkonomiMelding(
        bestillingsnummer: String,
        message: OkonomiBestillingMelding,
    ) {
        val record = StoredProducerRecord(
            config.bestillingTopic,
            bestillingsnummer.toByteArray(),
            Json.encodeToString(message).toByteArray(),
            null,
        )
        queries.kafkaProducerRecord.storeRecord(record)
    }

    fun handlinger(tilsagn: Tilsagn, ansatt: NavAnsatt): Set<TilsagnHandling> = db.session {
        val status = tilsagn.status

        val opprettelse = totrinnskontroll.getOrError(tilsagn.id, TotrinnskontrollType.TILSAGN_OPPRETTELSE)
        val annullering = totrinnskontroll.get(tilsagn.id, TotrinnskontrollType.TILSAGN_ANNULLERING)
        val oppgjor = totrinnskontroll.get(tilsagn.id, TotrinnskontrollType.TILSAGN_OPPGJOR)

        return setOfNotNull(
            TilsagnHandling.REDIGER.takeIf { status == TilsagnStatus.RETURNERT },
            TilsagnHandling.SLETT.takeIf { status == TilsagnStatus.RETURNERT },
            TilsagnHandling.ANNULLER.takeIf { status == TilsagnStatus.GODKJENT && tilsagn.belopBrukt.belop == 0 },
            TilsagnHandling.GJOR_OPP.takeIf { status == TilsagnStatus.GODKJENT && tilsagn.belopBrukt.belop > 0 },
            TilsagnHandling.GODKJENN.takeIf { status == TilsagnStatus.TIL_GODKJENNING },
            TilsagnHandling.RETURNER.takeIf { status == TilsagnStatus.TIL_GODKJENNING },
            TilsagnHandling.AVSLA_ANNULLERING.takeIf { status == TilsagnStatus.TIL_ANNULLERING },
            TilsagnHandling.GODKJENN_ANNULLERING.takeIf { status == TilsagnStatus.TIL_ANNULLERING },
            TilsagnHandling.AVSLA_OPPGJOR.takeIf { status == TilsagnStatus.TIL_OPPGJOR },
            TilsagnHandling.GODKJENN_OPPGJOR.takeIf { status == TilsagnStatus.TIL_OPPGJOR },
        )
            .filter {
                tilgangTilHandling(
                    handling = it,
                    ansatt = ansatt,
                    kostnadssted = tilsagn.kostnadssted.enhetsnummer,
                    opprettelse = opprettelse,
                    annullering = annullering,
                    tilOppgjor = oppgjor,
                )
            }
            .toSet()
    }

    companion object {
        fun tilgangTilHandling(
            handling: TilsagnHandling,
            ansatt: NavAnsatt,
            kostnadssted: NavEnhetNummer,
            opprettelse: Totrinnskontroll,
            annullering: Totrinnskontroll?,
            tilOppgjor: Totrinnskontroll?,
        ): Boolean {
            val erBeslutter = ansatt.hasKontorspesifikkRolle(Rolle.BESLUTTER_TILSAGN, setOf(kostnadssted))
            val erSaksbehandler = ansatt.hasGenerellRolle(Rolle.SAKSBEHANDLER_OKONOMI)

            return when (handling) {
                TilsagnHandling.REDIGER -> erSaksbehandler
                TilsagnHandling.GODKJENN -> erBeslutter && opprettelse.behandletAv != ansatt.navIdent
                TilsagnHandling.RETURNER -> erSaksbehandler || erBeslutter
                TilsagnHandling.SLETT -> erSaksbehandler
                TilsagnHandling.GJOR_OPP -> erSaksbehandler
                TilsagnHandling.GODKJENN_OPPGJOR -> erBeslutter && tilOppgjor?.behandletAv != ansatt.navIdent
                TilsagnHandling.AVSLA_OPPGJOR -> erBeslutter
                TilsagnHandling.ANNULLER -> erSaksbehandler
                TilsagnHandling.AVSLA_ANNULLERING -> erBeslutter
                TilsagnHandling.GODKJENN_ANNULLERING -> erBeslutter && annullering?.behandletAv != ansatt.navIdent
            }
        }
    }

    private fun QueryContext.updateFreeTextSearch(tilsagn: TilsagnDbo) {
        val fts = listOf(tilsagn.bestillingsnummer) +
            tilsagn.bestillingsnummer.replace("/", " ") +
            tilsagn.periode.toFreeTextSearch() +
            tilsagn.type.displayName()

        queries.tilsagn.setFreeTextSearch(tilsagn.id, fts)
    }
}
