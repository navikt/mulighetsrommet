package no.nav.mulighetsrommet.api.tilsagn

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.nel
import arrow.core.nonEmptyListOf
import arrow.core.right
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.aarsakerforklaring.AarsakerOgForklaringRequest
import no.nav.mulighetsrommet.api.avtale.mapper.satser
import no.nav.mulighetsrommet.api.avtale.model.findAvtaltSats
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsatt
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.navansatt.service.NavAnsattService
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnHandling
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
import no.nav.mulighetsrommet.api.tilsagn.model.UpsertTilsagn
import no.nav.mulighetsrommet.api.totrinnskontroll.TotrinnskontrollService
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollType
import no.nav.mulighetsrommet.api.utbetaling.service.UtbetalingService
import no.nav.mulighetsrommet.api.validation.validation
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.NOK
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.notifications.NotificationMetadata
import no.nav.mulighetsrommet.notifications.ScheduledNotification
import java.time.Instant
import java.util.UUID

class TilsagnService(
    val config: Config,
    private val db: ApiDatabase,
    private val okonomiService: UtbetalingService,
    private val totrinnskontroll: TotrinnskontrollService,
    private val navAnsattService: NavAnsattService,
) {
    data class Config(
        val gyldigTilsagnPeriode: Map<Tiltakskode, Periode>,
    )

    fun upsert(request: TilsagnRequest, agent: Agent): Either<List<FieldError>, Tilsagn> = db.transaction {
        val gjennomforing = queries.gjennomforing.getGjennomforingTiltaksadministrasjon(request.gjennomforingId)
        val previous = queries.tilsagn.get(requireNotNull(request.id))

        TilsagnValidator.validate(
            next = request,
            previous = previous,
            tiltakstypeNavn = gjennomforing.tiltakstype.navn,
            arrangorSlettet = gjennomforing.arrangor.slettet,
            gyldigTilsagnPeriode = config.gyldigTilsagnPeriode[gjennomforing.tiltakstype.tiltakskode],
            gjennomforingSluttDato = gjennomforing.sluttDato,
            prismodell = gjennomforing.prismodell,
        ).map { validated ->
            val upsert = UpsertTilsagn(
                id = request.id,
                gjennomforingId = request.gjennomforingId,
                type = request.type,
                periode = validated.periode,
                kostnadssted = validated.kostnadssted,
                beregning = validated.beregning,
                kommentar = request.kommentar?.trim(),
                beskrivelse = request.beskrivelse?.trim(),
                deltakere = request.deltakere?.map { UpsertTilsagn.Deltaker(it.deltakerId, it.innholdAnnet) },
            )
            okonomiService.upsertTilsagn(upsert, agent)
        }
    }

    fun slettTilsagn(id: UUID, navIdent: NavIdent): Either<List<FieldError>, Unit> = db.transaction {
        val tilsagn = queries.tilsagn.getAndAquireLock(id)
        if (tilsagn.status != TilsagnStatus.RETURNERT) {
            return FieldError.Companion.of("Kan ikke slette tilsagn som er godkjent").nel().left()
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
        okonomiService.setTilAnnullering(tilsagn, navIdent, request.aarsaker.map { it.name }, request.forklaring)
    }

    fun tilOppgjorRequest(
        id: UUID,
        navIdent: NavIdent,
        request: AarsakerOgForklaringRequest<TilsagnStatusAarsak>,
    ): Tilsagn = db.transaction {
        val tilsagn = queries.tilsagn.getAndAquireLock(id)
        okonomiService.setTilOppgjor(
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
                    TilsagnBeregningFri.Companion.beregn(
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
                        TilsagnBeregningFastSatsPerTiltaksplassPerManed.Companion.beregn(
                            TilsagnBeregningFastSatsPerTiltaksplassPerManed.Input(
                                periode = fallback.periode,
                                sats = fallback.sats,
                                antallPlasser = fallback.antallPlasser,
                            ),
                        )
                    }

                TilsagnBeregningType.PRIS_PER_MANEDSVERK ->
                    beregnTilsagnFallbackResolver(request)?.let { fallback ->
                        TilsagnBeregningPrisPerManedsverk.Companion.beregn(
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
                        TilsagnBeregningPrisPerUkesverk.Companion.beregn(
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
                        TilsagnBeregningPrisPerHeleUkesverk.Companion.beregn(
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
                        TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker.Companion.beregn(
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
        val periode = Periode.Companion.fromInclusiveDates(request.periodeStart, request.periodeSlutt)

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
    ): Either<List<FieldError>, Tilsagn> = db.transaction {
        validateAccessAndLockTilsagn(id, agent).flatMap {
            okonomiService.godkjennTilsagn(id, agent)
        }
    }

    fun returnerTilsagn(
        id: UUID,
        navIdent: NavIdent,
        aarsaker: List<TilsagnStatusAarsak>,
        forklaring: String?,
    ): Either<List<FieldError>, Tilsagn> = db.transaction {
        validateAccessAndLockTilsagn(id, navIdent).flatMap { tilsagn ->
            when (tilsagn.status) {
                TilsagnStatus.OPPGJORT, TilsagnStatus.ANNULLERT, TilsagnStatus.GODKJENT, TilsagnStatus.RETURNERT ->
                    FieldError.Companion.of("Tilsagnet kan ikke returneres fordi det har status ${tilsagn.status.beskrivelse}")
                        .nel().left()

                TilsagnStatus.TIL_GODKJENNING ->
                    okonomiService.returnerTilsagn(tilsagn, navIdent, aarsaker, forklaring)

                TilsagnStatus.TIL_ANNULLERING ->
                    okonomiService.avvisAnnullering(tilsagn, navIdent, aarsaker, forklaring).also { result ->
                        result.onRight { t ->
                            val annullering =
                                totrinnskontroll.getOrError(t.id, TotrinnskontrollType.TILSAGN_ANNULLERING)
                            if (annullering.behandletAv is NavIdent) {
                                sendNotifikasjonOmAvvistAnnullering(t, navIdent, annullering.behandletAv)
                            }
                        }
                    }

                TilsagnStatus.TIL_OPPGJOR ->
                    okonomiService.avvisOppgjor(tilsagn, navIdent, aarsaker, forklaring).also { result ->
                        result.onRight { t ->
                            val oppgjor = totrinnskontroll.getOrError(t.id, TotrinnskontrollType.TILSAGN_OPPGJOR)
                            if (oppgjor.behandletAv is NavIdent) {
                                sendNotifikasjonOmAvvistOppgjor(t, navIdent, oppgjor.behandletAv)
                            }
                        }
                    }
            }
        }
    }

    fun republishOpprettBestilling(bestillingsnummer: String): Tilsagn = db.transaction {
        okonomiService.republishOpprettBestilling(bestillingsnummer)
    }

    private fun QueryContext.validateAccessAndLockTilsagn(id: UUID, agent: Agent) = validation {
        val tilsagn = queries.tilsagn.getAndAquireLock(id)

        if (agent is NavIdent) {
            val ansatt = queries.ansatt.getByNavIdentOrError(agent)
            validate(
                ansatt.hasKontorspesifikkRolle(
                    Rolle.BESLUTTER_TILSAGN,
                    setOf(tilsagn.kostnadssted.enhetsnummer),
                ),
            ) {
                FieldError.Companion.of("Du kan ikke beslutte tilsagnet fordi du mangler budsjettmyndighet ved tilsagnets kostnadssted (${tilsagn.kostnadssted.navn})")
            }
        }

        tilsagn
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
            val beslutter = ansatt.hasKontorspesifikkRolle(Rolle.BESLUTTER_TILSAGN, setOf(kostnadssted))
            val saksbehandler = ansatt.hasGenerellRolle(Rolle.SAKSBEHANDLER_OKONOMI)

            return when (handling) {
                TilsagnHandling.REDIGER,
                TilsagnHandling.SLETT,
                TilsagnHandling.ANNULLER,
                TilsagnHandling.GJOR_OPP,
                -> saksbehandler

                TilsagnHandling.RETURNER,
                TilsagnHandling.AVSLA_ANNULLERING,
                TilsagnHandling.AVSLA_OPPGJOR,
                -> beslutter

                TilsagnHandling.GODKJENN -> {
                    beslutter && opprettelse.behandletAv != ansatt.navIdent
                }

                TilsagnHandling.GODKJENN_ANNULLERING -> {
                    beslutter && annullering?.behandletAv != ansatt.navIdent
                }

                TilsagnHandling.GODKJENN_OPPGJOR -> {
                    beslutter && tilOppgjor?.behandletAv != ansatt.navIdent
                }
            }
        }
    }
}
