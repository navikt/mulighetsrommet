package no.nav.mulighetsrommet.api.tilsagn

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.nel
import arrow.core.nonEmptyListOf
import arrow.core.right
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.aarsakerforklaring.AarsakerOgForklaringRequest
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.endringshistorikk.EndringshistorikkDto
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsatt
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.navansatt.service.NavAnsattService
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnHandling
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnDbo
import no.nav.mulighetsrommet.api.tilsagn.model.AvtalteSatser
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
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Besluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.validation.validation
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
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
) {
    data class Config(
        val bestillingTopic: String,
        val gyldigTilsagnPeriode: Map<Tiltakskode, Periode>,
    )

    fun upsert(request: TilsagnRequest, navIdent: NavIdent): Either<List<FieldError>, Tilsagn> = db.transaction {
        requireNotNull(request.id) { "id mangler" }

        val gjennomforing = queries.gjennomforing.getGruppetiltakOrError(request.gjennomforingId)
        val prismodell = requireNotNull(gjennomforing.prismodell) { "Gjennomføringen mangler prismodell" }
        val avtalteSatser = AvtalteSatser.getAvtalteSatser(gjennomforing.tiltakstype.tiltakskode, prismodell)

        val totrinnskontroll = Totrinnskontroll(
            id = UUID.randomUUID(),
            entityId = request.id,
            behandletAv = navIdent,
            aarsaker = emptyList(),
            forklaring = null,
            type = Totrinnskontroll.Type.OPPRETT,
            behandletTidspunkt = LocalDateTime.now(),
            besluttelse = null,
            besluttetAv = null,
            besluttetTidspunkt = null,
            besluttetAvNavn = null,
            behandletAvNavn = null,
        )
        val previous = queries.tilsagn.get(request.id)
        return TilsagnValidator
            .validate(
                next = request,
                previous = previous,
                tiltakstypeNavn = gjennomforing.tiltakstype.navn,
                arrangorSlettet = gjennomforing.arrangor.slettet,
                gyldigTilsagnPeriode = config.gyldigTilsagnPeriode[gjennomforing.tiltakstype.tiltakskode],
                gjennomforingSluttDato = gjennomforing.sluttDato,
                avtalteSatser = avtalteSatser,
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
                    belopBrukt = 0,
                    beregning = validated.beregning,
                    kommentar = request.kommentar?.trim(),
                    beskrivelse = request.beskrivelse?.trim(),
                )
            }
            .map { dbo ->
                queries.tilsagn.upsert(dbo)
                queries.totrinnskontroll.upsert(totrinnskontroll)

                val dto = queries.tilsagn.getOrError(dbo.id)

                logEndring("Sendt til godkjenning", dto, navIdent)
                dto
            }
    }

    fun getEndringshistorikk(id: UUID): EndringshistorikkDto = db.session {
        queries.endringshistorikk.getEndringshistorikk(DocumentClass.TILSAGN, id)
    }

    fun slettTilsagn(id: UUID, navIdent: NavIdent): Either<List<FieldError>, Unit> = db.transaction {
        val tilsagn = queries.tilsagn.getOrError(id)

        if (tilsagn.status != TilsagnStatus.RETURNERT) {
            return FieldError.of("Kan ikke slette tilsagn som er godkjent").nel().left()
        }

        val totrinnskontroll = queries.totrinnskontroll.getOrError(entityId = id, type = Totrinnskontroll.Type.OPPRETT)
        if (totrinnskontroll.besluttetAv == navIdent && totrinnskontroll.behandletAv is NavIdent) {
            sendNotifikasjonSlettetTilsagn(tilsagn, besluttetAv = navIdent, behandletAv = totrinnskontroll.behandletAv)
        }

        queries.tilsagn.delete(id)

        Unit.right()
    }

    fun tilAnnulleringRequest(
        id: UUID,
        navIdent: NavIdent,
        request: AarsakerOgForklaringRequest<TilsagnStatusAarsak>,
    ): Tilsagn = db.transaction {
        val tilsagn = queries.tilsagn.getOrError(id)

        setTilAnnullering(tilsagn, navIdent, request.aarsaker.map { it.name }, request.forklaring)
    }

    fun tilGjorOppRequest(
        id: UUID,
        navIdent: NavIdent,
        request: AarsakerOgForklaringRequest<TilsagnStatusAarsak>,
    ): Tilsagn = db.transaction {
        val tilsagn = queries.tilsagn.getOrError(id)

        setTilOppgjort(tilsagn, navIdent, request.aarsaker.map { it.name }, request.forklaring, "Sendt til oppgjør")
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
                                    belop = it.belop ?: 0,
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
        val sats: Int,
        val periode: Periode,
        val antallPlasser: Int,
        val antallTimerOppfolgingPerDeltaker: Int,
        val prisbetingelser: String?,
    )

    private fun beregnTilsagnFallbackResolver(request: BeregnTilsagnRequest): TilsagnBeregningFallbackResolver? = db.session {
        if (request.periodeStart == null || request.periodeSlutt == null) {
            return null
        }

        val gjennomforing = queries.gjennomforing.getGruppetiltakOrError(request.gjennomforingId)
        val prismodell = requireNotNull(gjennomforing.prismodell) { "Gjennomføringen mangler prismodell" }
        val avtalteSatser = AvtalteSatser.getAvtalteSatser(gjennomforing.tiltakstype.tiltakskode, prismodell)
        val sats = AvtalteSatser.findSats(avtalteSatser, request.periodeStart) ?: 0

        val antallPlasserFallback = request.beregning.antallPlasser ?: 0
        val antallTimerOppfolgingPerDeltakerFallback = request.beregning.antallTimerOppfolgingPerDeltaker ?: 0
        val periode = Periode.fromInclusiveDates(request.periodeStart, request.periodeSlutt)

        return TilsagnBeregningFallbackResolver(
            sats = sats,
            periode = periode,
            antallPlasser = antallPlasserFallback,
            antallTimerOppfolgingPerDeltaker = antallTimerOppfolgingPerDeltakerFallback,
            prisbetingelser = request.beregning.prisbetingelser,
        )
    }

    fun godkjennTilsagn(
        id: UUID,
        navIdent: NavIdent,
    ): Either<List<FieldError>, Tilsagn> = db.transaction {
        validateAccessToTilsagn(id, navIdent).flatMap { tilsagn ->
            when (tilsagn.status) {
                TilsagnStatus.OPPGJORT, TilsagnStatus.ANNULLERT, TilsagnStatus.GODKJENT, TilsagnStatus.RETURNERT,
                -> FieldError.of("Tilsagnet kan ikke godkjennes fordi det har status ${tilsagn.status.beskrivelse}")
                    .nel()
                    .left()

                TilsagnStatus.TIL_GODKJENNING -> godkjennTilsagn(tilsagn, navIdent).onRight {
                    publishOpprettBestilling(it)
                }

                TilsagnStatus.TIL_ANNULLERING -> annullerTilsagn(tilsagn, navIdent).onRight {
                    publishAnnullerBestilling(it)
                }

                TilsagnStatus.TIL_OPPGJOR -> gjorOppTilsagn(tilsagn, navIdent, "Tilsagn oppgjort").onRight {
                    publishGjorOppBestilling(it)
                }
            }
        }
    }

    fun returnerTilsagn(
        id: UUID,
        navIdent: NavIdent,
        aarsaker: List<TilsagnStatusAarsak>,
        forklaring: String?,
    ): Either<List<FieldError>, Tilsagn> = db.transaction {
        validateAccessToTilsagn(id, navIdent).flatMap { tilsagn ->
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
    }

    private fun QueryContext.validateAccessToTilsagn(id: UUID, navIdent: NavIdent) = validation {
        val tilsagn = queries.tilsagn.getOrError(id)

        val ansatt = queries.ansatt.getByNavIdentOrError(navIdent)
        validate(ansatt.hasKontorspesifikkRolle(Rolle.BESLUTTER_TILSAGN, setOf(tilsagn.kostnadssted.enhetsnummer))) {
            FieldError.of("Du kan ikke beslutte tilsagnet fordi du mangler budsjettmyndighet ved tilsagnets kostnadssted (${tilsagn.kostnadssted.navn})")
        }

        tilsagn
    }

    fun republishOpprettBestilling(bestillingsnummer: String): Tilsagn = db.transaction {
        val tilsagn = queries.tilsagn.getOrError(bestillingsnummer)
        publishOpprettBestilling(tilsagn)
        tilsagn
    }

    fun gjorOppTilsagnVedUtbetaling(
        id: UUID,
        behandletAv: Agent,
        besluttetAv: Agent,
        queryContext: QueryContext,
    ): Tilsagn {
        var tilsagn = queryContext.queries.tilsagn.getOrError(id)
        tilsagn = queryContext.setTilOppgjort(
            tilsagn,
            behandletAv,
            aarsaker = emptyList(),
            forklaring = null,
            operation = "Sendt til oppgjør ved behandling av utbetaling",
        )
        return queryContext.gjorOppTilsagn(
            tilsagn,
            besluttetAv,
            operation = "Tilsagn oppgjort ved attestering av utbetaling",
        ).getOrElse {
            // TODO returner valideringsfeil i stedet for å kaste exception
            throw IllegalStateException(it.first().detail)
        }
    }

    private fun QueryContext.godkjennTilsagn(
        tilsagn: Tilsagn,
        besluttetAv: NavIdent,
    ): Either<List<FieldError>, Tilsagn> {
        if (tilsagn.status != TilsagnStatus.TIL_GODKJENNING) {
            return FieldError.of("Tilsagnet må ha status ${TilsagnStatus.TIL_GODKJENNING} for å godkjennes")
                .nel()
                .left()
        }

        val opprettelse = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.OPPRETT)
        if (besluttetAv == opprettelse.behandletAv) {
            return FieldError.of("Du kan ikke beslutte et tilsagn du selv har opprettet").nel().left()
        }

        val besluttetOpprettelse = opprettelse.copy(
            besluttetAv = besluttetAv,
            besluttetTidspunkt = LocalDateTime.now(),
            besluttelse = Besluttelse.GODKJENT,
        )
        queries.totrinnskontroll.upsert(besluttetOpprettelse)
        queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.GODKJENT)

        val dto = queries.tilsagn.getOrError(tilsagn.id)
        logEndring("Tilsagn godkjent", dto, besluttetAv)
        return dto.right()
    }

    private fun QueryContext.returnerTilsagn(
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

        val opprettelse = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.OPPRETT)
        val avvistOpprettelse = opprettelse.copy(
            besluttetAv = besluttetAv,
            besluttetTidspunkt = LocalDateTime.now(),
            besluttelse = Besluttelse.AVVIST,
            aarsaker = aarsaker.map { it.name },
            forklaring = forklaring,
        )
        queries.totrinnskontroll.upsert(avvistOpprettelse)
        queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.RETURNERT)

        val dto = queries.tilsagn.getOrError(tilsagn.id)
        logEndring("Tilsagn returnert", dto, besluttetAv)
        return dto.right()
    }

    private fun QueryContext.setTilAnnullering(
        tilsagn: Tilsagn,
        behandletAv: Agent,
        aarsaker: List<String>,
        forklaring: String?,
    ): Tilsagn {
        require(tilsagn.status == TilsagnStatus.GODKJENT) {
            "Kan bare annullere godkjente tilsagn"
        }

        val totrinnskontroll = Totrinnskontroll(
            id = UUID.randomUUID(),
            entityId = tilsagn.id,
            behandletAv = behandletAv,
            aarsaker = aarsaker,
            forklaring = forklaring,
            type = Totrinnskontroll.Type.ANNULLER,
            behandletTidspunkt = LocalDateTime.now(),
            besluttelse = null,
            besluttetAv = null,
            besluttetTidspunkt = null,
            besluttetAvNavn = null,
            behandletAvNavn = null,
        )
        queries.totrinnskontroll.upsert(totrinnskontroll)
        queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.TIL_ANNULLERING)

        val dto = queries.tilsagn.getOrError(tilsagn.id)
        logEndring("Sendt til annullering", dto, behandletAv)
        return dto
    }

    private fun QueryContext.annullerTilsagn(
        tilsagn: Tilsagn,
        besluttetAv: NavIdent,
    ): Either<List<FieldError>, Tilsagn> {
        if (tilsagn.status != TilsagnStatus.TIL_ANNULLERING) {
            return FieldError.of("Tilsagnet må ha status ${TilsagnStatus.TIL_ANNULLERING} for at annullering skal godkjennes")
                .nel()
                .left()
        }

        val annullering = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.ANNULLER)
        if (besluttetAv == annullering.behandletAv) {
            return FieldError.of("Du kan ikke beslutte annullering du selv har opprettet").nel().left()
        }

        val besluttetAnnullering = annullering.copy(
            besluttetAv = besluttetAv,
            besluttetTidspunkt = LocalDateTime.now(),
            besluttelse = Besluttelse.GODKJENT,
        )
        queries.totrinnskontroll.upsert(besluttetAnnullering)
        queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.ANNULLERT)

        val dto = queries.tilsagn.getOrError(tilsagn.id)
        logEndring("Tilsagn annullert", dto, besluttetAv)
        return dto.right()
    }

    private fun QueryContext.avvisAnnullering(
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

        val annullering = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.ANNULLER)
        val avvistAnnullering = annullering.copy(
            besluttetAv = besluttetAv,
            besluttetTidspunkt = LocalDateTime.now(),
            besluttelse = Besluttelse.AVVIST,
            aarsaker = aarsaker.map { it.name },
            forklaring = forklaring,
        )
        queries.totrinnskontroll.upsert(avvistAnnullering)
        queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.GODKJENT)

        if (annullering.behandletAv is NavIdent) {
            sendNotifikasjonOmAvvistAnnullering(tilsagn, besluttetAv, annullering.behandletAv)
        }

        val dto = queries.tilsagn.getOrError(tilsagn.id)
        logEndring("Annullering avvist", dto, besluttetAv)
        return dto.right()
    }

    private fun QueryContext.setTilOppgjort(
        tilsagn: Tilsagn,
        agent: Agent,
        aarsaker: List<String>,
        forklaring: String?,
        operation: String,
    ): Tilsagn {
        require(tilsagn.status == TilsagnStatus.GODKJENT) {
            "Kan bare gjøre opp godkjente tilsagn"
        }

        val totrinnskontroll = Totrinnskontroll(
            id = UUID.randomUUID(),
            entityId = tilsagn.id,
            behandletAv = agent,
            aarsaker = aarsaker,
            forklaring = forklaring,
            type = Totrinnskontroll.Type.GJOR_OPP,
            behandletTidspunkt = LocalDateTime.now(),
            besluttelse = null,
            besluttetAv = null,
            besluttetTidspunkt = null,
            besluttetAvNavn = null,
            behandletAvNavn = null,
        )
        queries.totrinnskontroll.upsert(totrinnskontroll)
        queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.TIL_OPPGJOR)

        val dto = queries.tilsagn.getOrError(tilsagn.id)
        logEndring(operation, dto, agent)
        return dto
    }

    private fun QueryContext.gjorOppTilsagn(
        tilsagn: Tilsagn,
        besluttetAv: Agent,
        operation: String,
    ): Either<List<FieldError>, Tilsagn> {
        if (tilsagn.status != TilsagnStatus.TIL_OPPGJOR) {
            return FieldError.of("Tilsagnet må ha status ${TilsagnStatus.TIL_OPPGJOR} for at oppgjør skal godkjennes")
                .nel()
                .left()
        }

        val oppgjor = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.GJOR_OPP)
        if (besluttetAv is NavIdent && oppgjor.behandletAv == besluttetAv) {
            return FieldError.of("Du kan ikke beslutte oppgjør du selv har opprettet").nel().left()
        }

        val godkjentOppgjor = oppgjor.copy(
            besluttetAv = besluttetAv,
            besluttetTidspunkt = LocalDateTime.now(),
            besluttelse = Besluttelse.GODKJENT,
        )
        queries.totrinnskontroll.upsert(godkjentOppgjor)
        queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.OPPGJORT)

        val dto = queries.tilsagn.getOrError(tilsagn.id)
        logEndring(operation, dto, besluttetAv)
        return dto.right()
    }

    private fun avvisOppgjor(
        tilsagn: Tilsagn,
        besluttetAv: NavIdent,
        aarsaker: List<TilsagnStatusAarsak>,
        forklaring: String?,
    ): Either<List<FieldError>, Tilsagn> = db.transaction {
        if (tilsagn.status != TilsagnStatus.TIL_OPPGJOR) {
            return FieldError.of("Tilsagnet må ha status ${TilsagnStatus.TIL_OPPGJOR} for at oppgjør skal avvises")
                .nel()
                .left()
        }

        val oppgjor = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.GJOR_OPP)
        val avvistOppgjor = oppgjor.copy(
            besluttetAv = besluttetAv,
            besluttetTidspunkt = LocalDateTime.now(),
            besluttelse = Besluttelse.AVVIST,
            aarsaker = aarsaker.map { it.name },
            forklaring = forklaring,
        )
        queries.totrinnskontroll.upsert(avvistOppgjor)
        queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.GODKJENT)

        if (oppgjor.behandletAv is NavIdent) {
            sendNotifikasjonOmAvvistOppgjor(tilsagn, besluttetAv, oppgjor.behandletAv)
        }

        val dto = queries.tilsagn.getOrError(tilsagn.id)
        logEndring("Oppgjør avvist", dto, besluttetAv)
        return dto.right()
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

    private fun QueryContext.publishOpprettBestilling(tilsagn: Tilsagn) {
        val opprettelse = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.OPPRETT)
        check(opprettelse.besluttetAv != null && opprettelse.besluttetTidspunkt != null) {
            "Tilsagn id=${tilsagn.id} må være besluttet godkjent for å sendes til økonomi"
        }

        val gjennomforing = queries.gjennomforing.getGruppetiltakOrError(tilsagn.gjennomforing.id)

        val avtale = checkNotNull(gjennomforing.avtaleId?.let { queries.avtale.get(it) }) {
            "Gjennomføring ${gjennomforing.id} mangler avtale"
        }

        val bestilling = OpprettBestilling(
            bestillingsnummer = tilsagn.bestilling.bestillingsnummer,
            tilskuddstype = when (tilsagn.type) {
                TilsagnType.INVESTERING -> Tilskuddstype.TILTAK_INVESTERINGER
                else -> Tilskuddstype.TILTAK_DRIFTSTILSKUDD
            },
            tiltakskode = gjennomforing.tiltakstype.tiltakskode,
            arrangor = gjennomforing.arrangor.organisasjonsnummer,
            kostnadssted = tilsagn.kostnadssted.enhetsnummer,
            avtalenummer = avtale.sakarkivNummer?.value,
            belop = tilsagn.beregning.output.belop,
            periode = tilsagn.periode,
            behandletAv = opprettelse.behandletAv.toOkonomiPart(),
            behandletTidspunkt = opprettelse.behandletTidspunkt,
            besluttetAv = opprettelse.besluttetAv.toOkonomiPart(),
            besluttetTidspunkt = opprettelse.besluttetTidspunkt,
        )

        storeOkonomiMelding(bestilling.bestillingsnummer, OkonomiBestillingMelding.Bestilling(bestilling))
    }

    private fun QueryContext.publishAnnullerBestilling(tilsagn: Tilsagn) {
        val annullering = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.ANNULLER)
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

    private fun QueryContext.publishGjorOppBestilling(tilsagn: Tilsagn) {
        val oppgjor = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.GJOR_OPP)
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

        storeOkonomiMelding(tilsagn.bestilling.bestillingsnummer, OkonomiBestillingMelding.GjorOppBestilling(faktura))
    }

    private fun QueryContext.logEndring(
        operation: String,
        dto: Tilsagn,
        endretAv: Agent,
    ) {
        queries.endringshistorikk.logEndring(
            DocumentClass.TILSAGN,
            operation,
            endretAv,
            dto.id,
            LocalDateTime.now(),
        ) {
            Json.encodeToJsonElement(dto)
        }
    }

    private fun QueryContext.storeOkonomiMelding(bestillingsnummer: String, message: OkonomiBestillingMelding) {
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

        val opprettelse = queries.totrinnskontroll.getOrError(tilsagn.id, Totrinnskontroll.Type.OPPRETT)
        val annullering = queries.totrinnskontroll.get(tilsagn.id, Totrinnskontroll.Type.ANNULLER)
        val tilOppgjor = queries.totrinnskontroll.get(tilsagn.id, Totrinnskontroll.Type.GJOR_OPP)

        return setOfNotNull(
            TilsagnHandling.REDIGER.takeIf { status == TilsagnStatus.RETURNERT },
            TilsagnHandling.SLETT.takeIf { status == TilsagnStatus.RETURNERT },
            TilsagnHandling.ANNULLER.takeIf { status == TilsagnStatus.GODKJENT && tilsagn.belopBrukt == 0 },
            TilsagnHandling.GJOR_OPP.takeIf { status == TilsagnStatus.GODKJENT && tilsagn.belopBrukt > 0 },
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
                    tilOppgjor = tilOppgjor,
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
                ->
                    saksbehandler

                TilsagnHandling.RETURNER,
                TilsagnHandling.AVSLA_ANNULLERING,
                TilsagnHandling.AVSLA_OPPGJOR,
                ->
                    beslutter

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
