package no.nav.mulighetsrommet.api.utbetaling

import arrow.core.Either
import arrow.core.left
import arrow.core.nel
import arrow.core.right
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.TransactionalQueryContext
import no.nav.mulighetsrommet.api.arrangorflate.api.OpprettKravUtbetalingRequest
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsatt
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.TilsagnService
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Besluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingInputHelper.resolveAvtaltPrisPerTimeOppfolgingPerDeltaker
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingValidator.toAnnenAvtaltPris
import no.nav.mulighetsrommet.api.utbetaling.api.BesluttTotrinnskontrollRequest
import no.nav.mulighetsrommet.api.utbetaling.api.OpprettDelutbetalingerRequest
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingHandling
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingLinjeHandling
import no.nav.mulighetsrommet.api.utbetaling.db.DelutbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.model.*
import no.nav.mulighetsrommet.api.utbetaling.task.JournalforUtbetaling
import no.nav.mulighetsrommet.clamav.Vedlegg
import no.nav.mulighetsrommet.model.*
import no.nav.tiltak.okonomi.OkonomiBestillingMelding
import no.nav.tiltak.okonomi.OpprettFaktura
import no.nav.tiltak.okonomi.Tilskuddstype
import no.nav.tiltak.okonomi.toOkonomiPart
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

class UtbetalingService(
    private val config: Config,
    private val db: ApiDatabase,
    private val tilsagnService: TilsagnService,
    private val journalforUtbetaling: JournalforUtbetaling,
) {
    data class Config(
        val bestillingTopic: String,
    )

    private val log: Logger = LoggerFactory.getLogger(javaClass)

    fun godkjentAvArrangor(
        utbetalingId: UUID,
        kid: Kid?,
    ): Either<List<FieldError>, AutomatiskUtbetalingResult> = db.transaction {
        val utbetaling = queries.utbetaling.getAndAquireLock(utbetalingId)
        if (utbetaling.status != UtbetalingStatusType.GENERERT) {
            return FieldError.of("Utbetaling er allerede godkjent").nel().left()
        }

        queries.utbetaling.setGodkjentAvArrangor(utbetalingId, LocalDateTime.now())
        queries.utbetaling.setKid(utbetalingId, kid)
        queries.utbetaling.setStatus(utbetalingId, UtbetalingStatusType.INNSENDT)
        logEndring("Utbetaling sendt inn", getOrError(utbetalingId), Arrangor)

        scheduleJournalforUtbetaling(utbetalingId, vedlegg = emptyList())

        automatiskUtbetaling(utbetalingId)
            .also { log.info("Automatisk utbetaling for utbetaling=$utbetalingId resulterte i: $it") }
            .right()
    }

    fun opprettUtbetaling(
        utbetalingKrav: UtbetalingValidator.ValidertUtbetalingKrav,
        gjennomforing: Gjennomforing,
        agent: Agent,
    ): Either<List<FieldError>, Utbetaling> {
        val periode = Periode(utbetalingKrav.periodeStart, utbetalingKrav.periodeSlutt)
        return when (gjennomforing.avtalePrismodell) {
            PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK ->
                opprettAnnenAvtaltPrisUtbetaling(
                    utbetalingKrav.toAnnenAvtaltPris(
                        gjennomforingId = gjennomforing.id,
                        tilskuddstype = Tilskuddstype.TILTAK_INVESTERINGER,

                    ),
                    agent,
                    periode,
                )

            PrismodellType.ANNEN_AVTALT_PRIS ->
                opprettAnnenAvtaltPrisUtbetaling(
                    utbetalingKrav.toAnnenAvtaltPris(
                        gjennomforingId = gjennomforing.id,
                        tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
                    ),
                    agent,
                    periode,
                )

            PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER ->
                opprettAvtaltPrisPerTimeOppfolging(utbetalingKrav, gjennomforing, agent)

            PrismodellType.AVTALT_PRIS_PER_MANEDSVERK,
            PrismodellType.AVTALT_PRIS_PER_UKESVERK,
            PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK,
            -> Either.Left(
                listOf(
                    FieldError.of(
                        "Kan ikke opprette utbetaling for denne gjennomføringen manuelt",
                        OpprettKravUtbetalingRequest::tilsagnId,
                    ),
                ),
            )

            null -> Either.Left(
                listOf(
                    FieldError.of(
                        "Kan ikke opprette utbetaling for denne gjennomføringen manuelt",
                        OpprettKravUtbetalingRequest::tilsagnId,
                    ),
                ),
            )
        }
    }

    fun opprettAvtaltPrisPerTimeOppfolging(
        utbetalingKrav: UtbetalingValidator.ValidertUtbetalingKrav,
        gjennomforing: Gjennomforing,
        agent: Agent,
    ): Either<List<FieldError>, Utbetaling> = db.transaction {
        val periode = Periode(
            utbetalingKrav.periodeStart,
            utbetalingKrav.periodeSlutt,
        )
        val utbetalingInfo = resolveAvtaltPrisPerTimeOppfolgingPerDeltaker(gjennomforing, periode)
        val dbo = UtbetalingDbo(
            id = UUID.randomUUID(),
            gjennomforingId = gjennomforing.id,
            kontonummer = utbetalingKrav.kontonummer,
            kid = utbetalingKrav.kidNummer,
            beregning = UtbetalingBeregningPrisPerTimeOppfolging.beregn(
                input = UtbetalingBeregningPrisPerTimeOppfolging.Input(
                    periode = periode,
                    belop = utbetalingKrav.belop,
                    sats = utbetalingInfo.sats,
                    stengt = utbetalingInfo.stengtHosArrangor,
                    deltakelser = utbetalingInfo.deltakelsePerioder,
                ),
            ),
            periode = periode,
            innsender = agent,
            beskrivelse = "",
            tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
            godkjentAvArrangorTidspunkt = if (agent is Arrangor) {
                LocalDateTime.now()
            } else {
                null
            },
            status = UtbetalingStatusType.INNSENDT,
        )
        return opprettUtbetalingTransaction(dbo, utbetalingKrav.vedlegg, agent)
    }

    fun opprettAnnenAvtaltPrisUtbetaling(
        request: UtbetalingValidator.OpprettAnnenAvtaltPrisUtbetaling,
        agent: Agent,
    ): Either<List<FieldError>, Utbetaling> = opprettAnnenAvtaltPrisUtbetaling(
        request,
        agent,
        Periode.fromInclusiveDates(
            request.periodeStart,
            request.periodeSlutt,
        ),
    )

    fun opprettAnnenAvtaltPrisUtbetaling(
        request: UtbetalingValidator.OpprettAnnenAvtaltPrisUtbetaling,
        agent: Agent,
        periode: Periode,
    ): Either<List<FieldError>, Utbetaling> = db.transaction {
        val dbo = UtbetalingDbo(
            id = request.id,
            gjennomforingId = request.gjennomforingId,
            kontonummer = request.kontonummer,
            kid = request.kidNummer,
            beregning = UtbetalingBeregningFri.beregn(
                input = UtbetalingBeregningFri.Input(request.belop),
            ),
            periode = periode,
            innsender = agent,
            beskrivelse = request.beskrivelse,
            tilskuddstype = request.tilskuddstype,
            godkjentAvArrangorTidspunkt = if (agent is Arrangor) {
                LocalDateTime.now()
            } else {
                null
            },
            status = UtbetalingStatusType.INNSENDT,
        )

        return opprettUtbetalingTransaction(dbo, request.vedlegg, agent)
    }

    private fun TransactionalQueryContext.opprettUtbetalingTransaction(
        utbetaling: UtbetalingDbo,
        vedlegg: List<Vedlegg>,
        agent: Agent,
    ): Either<List<FieldError>, Utbetaling> {
        if (queries.utbetaling.get(utbetaling.id) != null) {
            return listOf(FieldError.of("Utbetalingen er allerede opprettet")).left()
        }

        queries.utbetaling.upsert(utbetaling)

        val dto = logEndring("Utbetaling sendt inn", getOrError(utbetaling.id), agent)

        if (agent is Arrangor) {
            scheduleJournalforUtbetaling(dto.id, vedlegg)
        }

        return dto.right()
    }

    fun opprettDelutbetalinger(
        request: OpprettDelutbetalingerRequest,
        navIdent: NavIdent,
    ): Either<List<FieldError>, Utbetaling> = db.transaction {
        val utbetaling = getOrError(request.utbetalingId)

        val opprettDelutbetalinger = request.delutbetalinger.map { req ->
            val tilsagn = queries.tilsagn.getOrError(req.tilsagnId)
            UtbetalingValidator.OpprettDelutbetaling(
                id = req.id,
                gjorOppTilsagn = req.gjorOppTilsagn,
                tilsagn = tilsagn,
                belop = req.belop,
            )
        }
        UtbetalingValidator
            .validateOpprettDelutbetalinger(utbetaling, opprettDelutbetalinger, request.begrunnelseMindreBetalt)
            .map { delutbetalinger ->
                // Slett de som ikke er med i requesten
                queries.delutbetaling.getByUtbetalingId(utbetaling.id)
                    .filter { delutbetaling -> delutbetaling.id !in request.delutbetalinger.map { it.id } }
                    .forEach { delutbetaling ->
                        require(delutbetaling.status == DelutbetalingStatus.RETURNERT) {
                            "Fatal! Delutbetaling kan ikke slettes fordi den har status: ${delutbetaling.status}"
                        }
                        queries.delutbetaling.delete(delutbetaling.id)
                    }

                delutbetalinger.forEach {
                    upsertDelutbetaling(
                        utbetaling,
                        it.tilsagn,
                        it.id,
                        requireNotNull(it.belop),
                        it.gjorOppTilsagn,
                        navIdent,
                    )
                }
                queries.utbetaling.setStatus(utbetaling.id, UtbetalingStatusType.TIL_ATTESTERING)
                queries.utbetaling.setBegrunnelseMindreBetalt(utbetaling.id, request.begrunnelseMindreBetalt)

                logEndring("Utbetaling sendt til attestering", getOrError(utbetaling.id), navIdent)
            }
    }

    fun besluttDelutbetaling(
        id: UUID,
        request: BesluttTotrinnskontrollRequest<DelutbetalingReturnertAarsak>,
        navIdent: NavIdent,
    ): Either<List<FieldError>, Delutbetaling> = db.transaction {
        val delutbetaling = queries.delutbetaling.getOrError(id)
        if (delutbetaling.status != DelutbetalingStatus.TIL_ATTESTERING) {
            return listOf(FieldError.of("Utbetaling er ikke satt til attestering")).left()
        }

        val kostnadssted = queries.tilsagn.getOrError(delutbetaling.tilsagnId).kostnadssted
        val ansatt = checkNotNull(queries.ansatt.getByNavIdent(navIdent))
        if (!ansatt.hasKontorspesifikkRolle(Rolle.ATTESTANT_UTBETALING, setOf(kostnadssted.enhetsnummer))) {
            return listOf(FieldError.of("Kan ikke attestere utbetalingen fordi du ikke er attestant ved tilsagnets kostnadssted (${kostnadssted.navn})")).left()
        }

        when (request.besluttelse) {
            Besluttelse.AVVIST -> {
                returnerDelutbetaling(delutbetaling, request.aarsaker, request.forklaring, navIdent)
            }

            Besluttelse.GODKJENT -> {
                val opprettelse = queries.totrinnskontroll.getOrError(delutbetaling.id, Totrinnskontroll.Type.OPPRETT)
                if (navIdent == opprettelse.behandletAv) {
                    return listOf(FieldError.of("Kan ikke attestere en utbetaling du selv har opprettet")).left()
                }

                godkjennDelutbetaling(delutbetaling, navIdent)
            }
        }

        queries.delutbetaling.getOrError(id).right()
    }

    fun republishFaktura(fakturanummer: String): Delutbetaling = db.transaction {
        val delutbetaling = queries.delutbetaling.getOrError(fakturanummer)
        publishOpprettFaktura(delutbetaling)
        delutbetaling
    }

    private fun TransactionalQueryContext.automatiskUtbetaling(utbetalingId: UUID): AutomatiskUtbetalingResult {
        val utbetaling = queries.utbetaling.getOrError(utbetalingId)

        when (utbetaling.beregning) {
            is UtbetalingBeregningFri,
            is UtbetalingBeregningPrisPerManedsverk,
            is UtbetalingBeregningPrisPerUkesverk,
            is UtbetalingBeregningPrisPerHeleUkesverk,
            is UtbetalingBeregningPrisPerTimeOppfolging,
            -> return AutomatiskUtbetalingResult.FEIL_PRISMODELL

            is UtbetalingBeregningFastSatsPerTiltaksplassPerManed,
            -> Unit
        }

        val relevanteTilsagn = queries.tilsagn.getAll(
            gjennomforingId = utbetaling.gjennomforing.id,
            statuser = listOf(TilsagnStatus.GODKJENT),
            typer = listOf(TilsagnType.TILSAGN, TilsagnType.EKSTRATILSAGN),
            periodeIntersectsWith = utbetaling.periode,
        )
        if (relevanteTilsagn.size != 1) {
            return AutomatiskUtbetalingResult.FEIL_ANTALL_TILSAGN
        }

        val tilsagn = relevanteTilsagn[0]
        if (tilsagn.gjenstaendeBelop() < utbetaling.beregning.output.belop) {
            return AutomatiskUtbetalingResult.IKKE_NOK_PENGER
        }

        val delutbetalinger = queries.delutbetaling.getByUtbetalingId(utbetalingId)
        if (delutbetalinger.isNotEmpty()) {
            return AutomatiskUtbetalingResult.DELUTBETALINGER_ALLEREDE_OPPRETTET
        }

        val delutbetalingId = UUID.randomUUID()
        upsertDelutbetaling(
            utbetaling = utbetaling,
            tilsagn = tilsagn,
            id = delutbetalingId,
            belop = utbetaling.beregning.output.belop,
            gjorOppTilsagn = tilsagn.periode.getLastInclusiveDate() in utbetaling.periode,
            behandletAv = Tiltaksadministrasjon,
        )

        val delutbetaling = queries.delutbetaling.getOrError(delutbetalingId)
        godkjennDelutbetaling(delutbetaling, Tiltaksadministrasjon)

        return AutomatiskUtbetalingResult.GODKJENT
    }

    private fun TransactionalQueryContext.upsertDelutbetaling(
        utbetaling: Utbetaling,
        tilsagn: Tilsagn,
        id: UUID,
        belop: Int,
        gjorOppTilsagn: Boolean,
        behandletAv: Agent,
    ) {
        require(tilsagn.status == TilsagnStatus.GODKJENT) {
            "Tilsagn er ikke godkjent id=${tilsagn.id} status=${tilsagn.status}"
        }

        val periode = requireNotNull(utbetaling.periode.intersect(tilsagn.periode)) {
            "Utbetalingsperiode og tilsagnsperiode overlapper ikke"
        }

        val delutbetaling = queries.delutbetaling.get(id)

        val lopenummer = delutbetaling?.lopenummer
            ?: queries.delutbetaling.getNextLopenummerByTilsagn(tilsagn.id)

        val fakturanummer = delutbetaling?.faktura?.fakturanummer
            ?: "${tilsagn.bestilling.bestillingsnummer}-$lopenummer"

        val dbo = DelutbetalingDbo(
            id = id,
            utbetalingId = utbetaling.id,
            tilsagnId = tilsagn.id,
            status = DelutbetalingStatus.TIL_ATTESTERING,
            periode = periode,
            belop = belop,
            gjorOppTilsagn = gjorOppTilsagn,
            lopenummer = lopenummer,
            fakturanummer = fakturanummer,
            fakturaStatus = null,
            fakturaStatusSistOppdatert = LocalDateTime.now(),
        )

        queries.delutbetaling.upsert(dbo)
        queries.totrinnskontroll.upsert(
            Totrinnskontroll(
                id = UUID.randomUUID(),
                entityId = id,
                behandletAv = behandletAv,
                aarsaker = emptyList(),
                forklaring = null,
                type = Totrinnskontroll.Type.OPPRETT,
                behandletTidspunkt = LocalDateTime.now(),
                besluttelse = null,
                besluttetAv = null,
                besluttetTidspunkt = null,
                behandletAvNavn = null,
                besluttetAvNavn = null,
            ),
        )
    }

    private fun TransactionalQueryContext.godkjennDelutbetaling(
        delutbetaling: Delutbetaling,
        besluttetAv: Agent,
    ) {
        val opprettelse = queries.totrinnskontroll.getOrError(delutbetaling.id, Totrinnskontroll.Type.OPPRETT)
        require(opprettelse.besluttetAv == null) {
            "Utbetaling er allerede besluttet"
        }

        queries.delutbetaling.setStatus(delutbetaling.id, DelutbetalingStatus.GODKJENT)
        queries.totrinnskontroll.upsert(
            opprettelse.copy(
                besluttetAv = besluttetAv,
                besluttelse = Besluttelse.GODKJENT,
                besluttetTidspunkt = LocalDateTime.now(),
                aarsaker = emptyList(),
                forklaring = null,
            ),
        )

        val utbetaling = getOrError(delutbetaling.utbetalingId)
        val delutbetalinger = queries.delutbetaling.getByUtbetalingId(delutbetaling.utbetalingId)

        delutbetalinger.forEach {
            val tilsagn = queries.tilsagn.getOrError(it.tilsagnId)
            if (tilsagn.status != TilsagnStatus.GODKJENT) {
                return returnerDelutbetaling(
                    it,
                    listOf(DelutbetalingReturnertAarsak.TILSAGN_FEIL_STATUS),
                    null,
                    Tiltaksadministrasjon,
                )
            }
        }

        if (delutbetalinger.all { it.status == DelutbetalingStatus.GODKJENT }) {
            godkjennUtbetaling(utbetaling, delutbetalinger)
        }
    }

    private fun TransactionalQueryContext.godkjennUtbetaling(
        utbetaling: Utbetaling,
        delutbetalinger: List<Delutbetaling>,
    ) {
        queries.delutbetaling.setStatusForDelutbetalingerForBetaling(
            utbetaling.id,
            DelutbetalingStatus.OVERFORT_TIL_UTBETALING,
        )

        delutbetalinger.forEach { delutbetaling ->
            val tilsagn = queries.tilsagn.getOrError(delutbetaling.tilsagnId)
            val benyttetBelop = tilsagn.belopBrukt + delutbetaling.belop
            queries.tilsagn.setBruktBelop(tilsagn.id, benyttetBelop)
            if (delutbetaling.gjorOppTilsagn || benyttetBelop == tilsagn.beregning.output.belop) {
                tilsagnService.gjorOppAutomatisk(delutbetaling.tilsagnId, this)
            }
            publishOpprettFaktura(delutbetaling)
        }

        queries.utbetaling.setStatus(utbetaling.id, UtbetalingStatusType.FERDIG_BEHANDLET)
        logEndring("Overført til utbetaling", utbetaling, Tiltaksadministrasjon)
    }

    private fun TransactionalQueryContext.returnerDelutbetaling(
        delutbetaling: Delutbetaling,
        aarsaker: List<DelutbetalingReturnertAarsak>,
        forklaring: String?,
        besluttetAv: Agent,
    ) {
        setReturnertDelutbetaling(delutbetaling, aarsaker, forklaring, besluttetAv)

        // Set også de resterende delutbetalingene som returnert
        queries.delutbetaling.getByUtbetalingId(delutbetaling.utbetalingId)
            .filter { it.id != delutbetaling.id }
            .forEach {
                setReturnertDelutbetaling(
                    it,
                    listOf(DelutbetalingReturnertAarsak.PROPAGERT_RETUR),
                    null,
                    Tiltaksadministrasjon,
                )
            }

        queries.utbetaling.setStatus(delutbetaling.utbetalingId, UtbetalingStatusType.RETURNERT)
        logEndring(
            "Utbetaling returnert",
            getOrError(delutbetaling.utbetalingId),
            besluttetAv,
        )
    }

    private fun TransactionalQueryContext.setReturnertDelutbetaling(
        delutbetaling: Delutbetaling,
        aarsaker: List<DelutbetalingReturnertAarsak>,
        forklaring: String?,
        besluttetAv: Agent,
    ) {
        val opprettelse = queries.totrinnskontroll.getOrError(delutbetaling.id, Totrinnskontroll.Type.OPPRETT)
        queries.delutbetaling.setStatus(delutbetaling.id, DelutbetalingStatus.RETURNERT)
        queries.totrinnskontroll.upsert(
            opprettelse.copy(
                besluttetAv = besluttetAv,
                besluttelse = Besluttelse.AVVIST,
                aarsaker = aarsaker.map { it.name },
                forklaring = forklaring,
                besluttetTidspunkt = LocalDateTime.now(),
            ),
        )
    }

    private fun TransactionalQueryContext.logEndring(
        operation: String,
        dto: Utbetaling,
        endretAv: Agent,
    ): Utbetaling {
        queries.endringshistorikk.logEndring(
            DocumentClass.UTBETALING,
            operation,
            endretAv,
            dto.id,
            LocalDateTime.now(),
        ) {
            Json.encodeToJsonElement(dto)
        }
        return dto
    }

    private fun TransactionalQueryContext.scheduleJournalforUtbetaling(utbetalingId: UUID, vedlegg: List<Vedlegg>) {
        journalforUtbetaling.schedule(
            utbetalingId = utbetalingId,
            startTime = Instant.now(),
            tx = session,
            vedlegg = vedlegg,
        )
    }

    private fun TransactionalQueryContext.publishOpprettFaktura(delutbetaling: Delutbetaling) {
        check(delutbetaling.status == DelutbetalingStatus.GODKJENT) {
            "Delutbetaling må være godkjent for "
        }

        val opprettelse = queries.totrinnskontroll.getOrError(delutbetaling.id, Totrinnskontroll.Type.OPPRETT)
        check(opprettelse.besluttetAv != null && opprettelse.besluttetTidspunkt != null && opprettelse.besluttelse == Besluttelse.GODKJENT) {
            "Delutbetaling id=${delutbetaling.id} må være besluttet godkjent for å sendes til økonomi"
        }

        val utbetaling = queries.utbetaling.getOrError(delutbetaling.utbetalingId)
        val kontonummer = checkNotNull(utbetaling.betalingsinformasjon.kontonummer) {
            "Kontonummer mangler for utbetaling med id=${delutbetaling.utbetalingId}"
        }

        val tilsagn = queries.tilsagn.getOrError(delutbetaling.tilsagnId)

        val beskrivelse = """
            Tiltakstype: ${tilsagn.tiltakstype.navn}
            Periode: ${tilsagn.periode.formatPeriode()}
            Tilsagnsnummer: ${tilsagn.bestilling.bestillingsnummer}
        """.trimIndent()

        val faktura = OpprettFaktura(
            fakturanummer = delutbetaling.faktura.fakturanummer,
            bestillingsnummer = tilsagn.bestilling.bestillingsnummer,
            betalingsinformasjon = OpprettFaktura.Betalingsinformasjon(
                kontonummer = kontonummer,
                kid = utbetaling.betalingsinformasjon.kid,
            ),
            belop = delutbetaling.belop,
            periode = delutbetaling.periode,
            behandletAv = opprettelse.behandletAv.toOkonomiPart(),
            behandletTidspunkt = opprettelse.behandletTidspunkt,
            besluttetAv = opprettelse.besluttetAv.toOkonomiPart(),
            besluttetTidspunkt = opprettelse.besluttetTidspunkt,
            gjorOppBestilling = delutbetaling.gjorOppTilsagn,
            beskrivelse = beskrivelse,
        )

        queries.delutbetaling.setSendtTilOkonomi(
            delutbetaling.utbetalingId,
            delutbetaling.tilsagnId,
            LocalDateTime.now(),
        )
        val message = OkonomiBestillingMelding.Faktura(faktura)
        storeOkonomiMelding(faktura.bestillingsnummer, message)
    }

    private fun TransactionalQueryContext.storeOkonomiMelding(
        bestillingsnummer: String,
        message: OkonomiBestillingMelding,
    ) {
        log.info("Lagrer faktura for delutbeatling med bestillingsnummer=$bestillingsnummer for publisering på kafka")

        val record = StoredProducerRecord(
            config.bestillingTopic,
            bestillingsnummer.toByteArray(),
            Json.encodeToString(message).toByteArray(),
            null,
        )
        queries.kafkaProducerRecord.storeRecord(record)
    }

    private fun QueryContext.getOrError(id: UUID): Utbetaling {
        return queries.utbetaling.getOrError(id)
    }

    companion object {
        fun utbetalingHandlinger(utbetaling: Utbetaling, ansatt: NavAnsatt) = setOfNotNull(
            UtbetalingHandling.SEND_TIL_ATTESTERING.takeIf {
                when (utbetaling.status) {
                    UtbetalingStatusType.INNSENDT,
                    UtbetalingStatusType.RETURNERT,
                    -> true

                    UtbetalingStatusType.FERDIG_BEHANDLET,
                    UtbetalingStatusType.GENERERT,
                    UtbetalingStatusType.TIL_ATTESTERING,
                    -> false
                }
            },
        )
            .filter {
                tilgangTilHandling(handling = it, ansatt = ansatt)
            }
            .toSet()

        fun linjeHandlinger(
            delutbetaling: Delutbetaling,
            opprettelse: Totrinnskontroll,
            kostnadssted: NavEnhetNummer,
            ansatt: NavAnsatt,
        ): Set<UtbetalingLinjeHandling> {
            return setOfNotNull(
                UtbetalingLinjeHandling.ATTESTER.takeIf { delutbetaling.status == DelutbetalingStatus.TIL_ATTESTERING },
                UtbetalingLinjeHandling.RETURNER.takeIf { delutbetaling.status == DelutbetalingStatus.TIL_ATTESTERING },
            )
                .filter {
                    tilgangTilHandling(
                        handling = it,
                        ansatt = ansatt,
                        kostnadssted = kostnadssted,
                        opprettelse = opprettelse,
                    )
                }
                .toSet()
        }

        fun tilgangTilHandling(handling: UtbetalingHandling, ansatt: NavAnsatt): Boolean {
            return when (handling) {
                UtbetalingHandling.SEND_TIL_ATTESTERING -> ansatt.hasGenerellRolle(Rolle.SAKSBEHANDLER_OKONOMI)
            }
        }

        fun tilgangTilHandling(
            handling: UtbetalingLinjeHandling,
            ansatt: NavAnsatt,
            kostnadssted: NavEnhetNummer,
            opprettelse: Totrinnskontroll,
        ): Boolean {
            val erBeslutter = ansatt.hasKontorspesifikkRolle(
                Rolle.ATTESTANT_UTBETALING,
                setOf(kostnadssted),
            )
            val erSaksbehandler = ansatt.hasGenerellRolle(Rolle.SAKSBEHANDLER_OKONOMI)

            return when (handling) {
                UtbetalingLinjeHandling.ATTESTER ->
                    erBeslutter && opprettelse.behandletAv != ansatt.navIdent

                UtbetalingLinjeHandling.RETURNER -> erBeslutter
                UtbetalingLinjeHandling.SEND_TIL_ATTESTERING -> erSaksbehandler
            }
        }
    }
}
