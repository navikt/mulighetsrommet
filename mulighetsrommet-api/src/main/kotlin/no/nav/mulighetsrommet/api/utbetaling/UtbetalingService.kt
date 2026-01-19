package no.nav.mulighetsrommet.api.utbetaling

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.nel
import arrow.core.right
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord
import no.nav.common.kafka.util.KafkaUtils
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.TransactionalQueryContext
import no.nav.mulighetsrommet.api.arrangorflate.api.OpprettKravUtbetalingRequest
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingGruppetiltak
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
import no.nav.mulighetsrommet.api.utbetaling.api.OpprettDelutbetalingerRequest
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingHandling
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingLinjeHandling
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingType
import no.nav.mulighetsrommet.api.utbetaling.db.DelutbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.model.AutomatiskUtbetalingResult
import no.nav.mulighetsrommet.api.utbetaling.model.Delutbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingReturnertAarsak
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFastSatsPerTiltaksplassPerManed
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerHeleUkesverk
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerManedsverk
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerTimeOppfolging
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerUkesverk
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.api.utbetaling.task.JournalforUtbetaling
import no.nav.mulighetsrommet.api.validation.validation
import no.nav.mulighetsrommet.clamav.Vedlegg
import no.nav.mulighetsrommet.kafka.KAFKA_CONSUMER_RECORD_PROCESSOR_SCHEDULED_AT
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.Arrangor
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltaksadministrasjon
import no.nav.tiltak.okonomi.FakturaStatusType
import no.nav.tiltak.okonomi.OkonomiBestillingMelding
import no.nav.tiltak.okonomi.OpprettFaktura
import no.nav.tiltak.okonomi.Tilskuddstype
import no.nav.tiltak.okonomi.toOkonomiPart
import org.apache.kafka.common.header.internals.RecordHeaders
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

class UtbetalingService(
    private val config: Config,
    private val db: ApiDatabase,
    private val tilsagnService: TilsagnService,
    private val journalforUtbetaling: JournalforUtbetaling,
) {
    data class Config(
        val bestillingTopic: String,
        val tidligstTidspunktForUtbetaling: TidligstTidspunktForUtbetalingCalculator,
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
            .also { result -> log.info("Automatisk utbetaling for utbetaling=$utbetalingId resulterte i: $result") }
            .right()
    }

    fun opprettUtbetaling(
        utbetalingKrav: UtbetalingValidator.ValidertUtbetalingKrav,
        gjennomforing: GjennomforingGruppetiltak,
        agent: Agent,
    ): Either<List<FieldError>, Utbetaling> {
        val periode = Periode(utbetalingKrav.periodeStart, utbetalingKrav.periodeSlutt)
        return when (gjennomforing.prismodell?.type) {
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
            null,
            -> Either.Left(
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
        gjennomforing: GjennomforingGruppetiltak,
        agent: Agent,
    ): Either<List<FieldError>, Utbetaling> = db.transaction {
        val periode = Periode(utbetalingKrav.periodeStart, utbetalingKrav.periodeSlutt)
        val utbetalingInfo = resolveAvtaltPrisPerTimeOppfolgingPerDeltaker(gjennomforing, periode)
        val dbo = UtbetalingDbo(
            id = UUID.randomUUID(),
            gjennomforingId = gjennomforing.id,
            status = UtbetalingStatusType.INNSENDT,
            kontonummer = utbetalingKrav.kontonummer,
            kid = utbetalingKrav.kidNummer,
            beregning = UtbetalingBeregningPrisPerTimeOppfolging.beregn(
                input = UtbetalingBeregningPrisPerTimeOppfolging.Input(
                    satser = utbetalingInfo.satser,
                    belop = utbetalingKrav.belop,
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
            utbetalesTidligstTidspunkt = null,
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
            status = UtbetalingStatusType.INNSENDT,
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
            utbetalesTidligstTidspunkt = null,
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

        val delutbetalingTilsagn = request.delutbetalinger.associate { req ->
            val tilsagn = queries.tilsagn.getOrError(req.tilsagnId)
            req.id to tilsagn
        }

        UtbetalingValidator
            .validateOpprettDelutbetalinger(
                utbetaling,
                request.delutbetalinger.map { req ->
                    val tilsagn = requireNotNull(delutbetalingTilsagn[req.id])
                    UtbetalingValidator.OpprettDelutbetaling(
                        id = req.id,
                        gjorOppTilsagn = req.gjorOppTilsagn,
                        tilsagn = UtbetalingValidator.OpprettDelutbetaling.Tilsagn(
                            status = tilsagn.status,
                            gjenstaendeBelop = tilsagn.gjenstaendeBelop().belop, // TODO: Ta med valuta
                        ),
                        belop = req.belop,
                    )
                },
                request.begrunnelseMindreBetalt,
            )
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
                        requireNotNull(delutbetalingTilsagn[it.id]),
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

    fun godkjennDelutbetaling(
        id: UUID,
        navIdent: NavIdent,
    ): Either<List<FieldError>, Delutbetaling> = db.transaction {
        validateAccessToDelutbetaling(id, navIdent).flatMap { delutbetaling ->
            godkjennDelutbetaling(delutbetaling, navIdent).map { queries.delutbetaling.getOrError(id) }
        }
    }

    fun returnerDelutbetaling(
        id: UUID,
        aarsaker: List<DelutbetalingReturnertAarsak>,
        forklaring: String?,
        navIdent: NavIdent,
    ): Either<List<FieldError>, Delutbetaling> = db.transaction {
        validateAccessToDelutbetaling(id, navIdent).map { delutbetaling ->
            returnerDelutbetaling(delutbetaling, aarsaker, forklaring, navIdent)
            queries.delutbetaling.getOrError(id)
        }
    }

    private fun QueryContext.validateAccessToDelutbetaling(id: UUID, navIdent: NavIdent) = validation {
        val delutbetaling = queries.delutbetaling.getOrError(id)
        validate(delutbetaling.status == DelutbetalingStatus.TIL_ATTESTERING) {
            FieldError.of("Utbetaling er ikke satt til attestering")
        }

        val kostnadssted = queries.tilsagn.getOrError(delutbetaling.tilsagnId).kostnadssted
        val ansatt = queries.ansatt.getByNavIdentOrError(navIdent)
        validate(ansatt.hasKontorspesifikkRolle(Rolle.ATTESTANT_UTBETALING, setOf(kostnadssted.enhetsnummer))) {
            FieldError.of("Kan ikke attestere utbetalingen fordi du ikke er attestant ved tilsagnets kostnadssted (${kostnadssted.navn})")
        }

        delutbetaling
    }

    fun slettKorreksjon(id: UUID): Either<List<FieldError>, Unit> = db.transaction {
        val utbetaling = getOrError(id)
        when (utbetaling.status) {
            UtbetalingStatusType.RETURNERT,
            UtbetalingStatusType.INNSENDT,
            -> Unit

            UtbetalingStatusType.GENERERT,
            UtbetalingStatusType.TIL_ATTESTERING,
            UtbetalingStatusType.FERDIG_BEHANDLET,
            UtbetalingStatusType.DELVIS_UTBETALT,
            UtbetalingStatusType.UTBETALT,
            UtbetalingStatusType.AVBRUTT,
            ->
                return FieldError.root(
                    "Kan ikke slette utbetaling fordi den har status: ${utbetaling.status}",
                ).nel().left()
        }
        if (UtbetalingType.from(utbetaling) != UtbetalingType.KORRIGERING) {
            return FieldError.root("Kan kun slette korreksjoner").nel().left()
        }
        queries.delutbetaling.getByUtbetalingId(id)
            .forEach { delutbetaling ->
                if (delutbetaling.status != DelutbetalingStatus.RETURNERT) {
                    return FieldError.root("Delutbetaling var i feil status").nel().left()
                }
                queries.delutbetaling.delete(delutbetaling.id)
            }

        queries.utbetaling.delete(id).right()
    }

    fun republishFaktura(fakturanummer: String): Delutbetaling = db.transaction {
        val delutbetaling = queries.delutbetaling.getOrError(fakturanummer)
        publishOpprettFaktura(delutbetaling)
        delutbetaling
    }

    fun oppdaterFakturaStatus(
        fakturanummer: String,
        nyStatus: FakturaStatusType,
        fakturaStatusSistOppdatert: LocalDateTime?,
    ): Unit = db.transaction {
        val originalDelutbetaling = queries.delutbetaling.getOrError(fakturanummer)
        if (originalDelutbetaling.faktura.statusSistOppdatert != null &&
            fakturaStatusSistOppdatert != null &&
            originalDelutbetaling.faktura.statusSistOppdatert.isAfter(fakturaStatusSistOppdatert)
        ) {
            return
        }

        queries.delutbetaling.setFakturaStatus(fakturanummer, nyStatus, fakturaStatusSistOppdatert)

        when (nyStatus) {
            FakturaStatusType.FEILET,
            FakturaStatusType.SENDT,
            FakturaStatusType.IKKE_BETALT,
            -> {
                check(!originalDelutbetaling.faktura.erUtbetalt()) {
                    "Delutbetaling ${originalDelutbetaling.id} faktura status er ${originalDelutbetaling.faktura.status}, ny status $nyStatus"
                }
                queries.delutbetaling.setStatus(fakturanummer, DelutbetalingStatus.OVERFORT_TIL_UTBETALING)
            }

            FakturaStatusType.DELVIS_BETALT,
            FakturaStatusType.FULLT_BETALT,
            -> {
                queries.delutbetaling.setStatus(fakturanummer, DelutbetalingStatus.UTBETALT)
                oppdaterUtbetalingForUtbetaltDelutbetaling(originalDelutbetaling.utbetalingId)
                if (!originalDelutbetaling.faktura.erUtbetalt()) {
                    logDelutbetalingUtbetalt(originalDelutbetaling, fakturaStatusSistOppdatert)
                }
            }
        }
    }

    private fun TransactionalQueryContext.logDelutbetalingUtbetalt(
        delutbetaling: Delutbetaling,
        fakturaStatusSistOppdatert: LocalDateTime?,
    ) {
        val tilsagn = queries.tilsagn.getOrError(delutbetaling.tilsagnId)
        val utbetaling = queries.utbetaling.getOrError(delutbetaling.utbetalingId)

        logEndring(
            "Betaling for tilsagn ${tilsagn.bestilling.bestillingsnummer} er utbetalt",
            utbetaling,
            Tiltaksadministrasjon,
            fakturaStatusSistOppdatert ?: LocalDateTime.now(),
        )
    }

    private fun TransactionalQueryContext.oppdaterUtbetalingForUtbetaltDelutbetaling(
        utbetalingId: UUID,
    ) {
        val utbetaling = queries.utbetaling.getAndAquireLock(utbetalingId)
        val delutbetalinger = queries.delutbetaling.getByUtbetalingId(utbetaling.id)

        val oppdatertUtbetalingStatus = when {
            delutbetalinger.all { it.status == DelutbetalingStatus.UTBETALT } -> UtbetalingStatusType.UTBETALT
            delutbetalinger.any { it.status == DelutbetalingStatus.UTBETALT } -> UtbetalingStatusType.DELVIS_UTBETALT
            else -> utbetaling.status
        }
        if (utbetaling.status != oppdatertUtbetalingStatus) {
            queries.utbetaling.setStatus(utbetaling.id, oppdatertUtbetalingStatus)
        }
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
        if (tilsagn.gjenstaendeBelop().belop < utbetaling.beregning.output.belop) {
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
        return godkjennDelutbetaling(delutbetaling, Tiltaksadministrasjon).fold(
            { errors ->
                log.error("Uventet valideringsfeil oppsto under automatisk utbetaling: $errors")
                AutomatiskUtbetalingResult.VALIDERINGSFEIL
            },
            { AutomatiskUtbetalingResult.GODKJENT },
        )
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
    ): Either<List<FieldError>, Utbetaling> {
        val opprettelse = queries.totrinnskontroll.getOrError(delutbetaling.id, Totrinnskontroll.Type.OPPRETT)
        require(opprettelse.besluttetAv == null) {
            "Utbetaling er allerede besluttet"
        }

        if (besluttetAv is NavIdent && opprettelse.behandletAv is NavIdent && besluttetAv == opprettelse.behandletAv) {
            return listOf(FieldError.of("Kan ikke attestere en utbetaling du selv har opprettet")).left()
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

        val delutbetalinger = queries.delutbetaling.getByUtbetalingId(delutbetaling.utbetalingId)

        delutbetalinger.forEach {
            val tilsagn = queries.tilsagn.getOrError(it.tilsagnId)
            if (tilsagn.status != TilsagnStatus.GODKJENT) {
                return returnerDelutbetaling(
                    it,
                    listOf(DelutbetalingReturnertAarsak.TILSAGN_FEIL_STATUS),
                    null,
                    Tiltaksadministrasjon,
                ).right()
            }
        }

        return if (delutbetalinger.all { it.status == DelutbetalingStatus.GODKJENT }) {
            godkjennUtbetaling(delutbetaling.utbetalingId, delutbetalinger)
        } else {
            getOrError(delutbetaling.utbetalingId)
        }.right()
    }

    private fun TransactionalQueryContext.godkjennUtbetaling(
        id: UUID,
        delutbetalinger: List<Delutbetaling>,
    ): Utbetaling {
        queries.delutbetaling.setStatusForDelutbetalingerForBetaling(id, DelutbetalingStatus.OVERFORT_TIL_UTBETALING)

        delutbetalinger.forEach { delutbetaling ->
            val tilsagn = queries.tilsagn.getOrError(delutbetaling.tilsagnId)
            val benyttetBelop = tilsagn.belopBrukt.belop + delutbetaling.belop
            val opprettelse = queries.totrinnskontroll.getOrError(delutbetaling.id, Totrinnskontroll.Type.OPPRETT)
            queries.tilsagn.setBruktBelop(tilsagn.id, benyttetBelop)
            if (delutbetaling.gjorOppTilsagn || benyttetBelop == tilsagn.beregning.output.pris.belop) {
                tilsagnService.gjorOppTilsagnVedUtbetaling(
                    delutbetaling.tilsagnId,
                    behandletAv = opprettelse.behandletAv,
                    besluttetAv = requireNotNull(opprettelse.besluttetAv),
                    this,
                )
            }
            publishOpprettFaktura(delutbetaling)
        }

        queries.utbetaling.setStatus(id, UtbetalingStatusType.FERDIG_BEHANDLET)
        return logEndring("Overført til utbetaling", getOrError(id), Tiltaksadministrasjon)
    }

    private fun TransactionalQueryContext.returnerDelutbetaling(
        delutbetaling: Delutbetaling,
        aarsaker: List<DelutbetalingReturnertAarsak>,
        forklaring: String?,
        besluttetAv: Agent,
    ): Utbetaling {
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
        return logEndring(
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
        timestamp: LocalDateTime = LocalDateTime.now(),
    ): Utbetaling {
        queries.endringshistorikk.logEndring(
            DocumentClass.UTBETALING,
            operation,
            endretAv,
            dto.id,
            timestamp,
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
            Instant.now(),
        )

        val tidspunktForUtbetaling = delutbetaling.faktura.utbetalesTidligstTidspunkt
            ?: config.tidligstTidspunktForUtbetaling.calculate(tilsagn.tiltakstype.tiltakskode, faktura.periode)
        val message = OkonomiBestillingMelding.Faktura(faktura)
        storeOkonomiMelding(faktura.bestillingsnummer, message, tidspunktForUtbetaling)
    }

    private fun TransactionalQueryContext.storeOkonomiMelding(
        bestillingsnummer: String,
        message: OkonomiBestillingMelding,
        tidspunktForUtbetaling: Instant?,
    ) {
        log.info("Lagrer faktura for delutbetaling med bestillingsnummer=$bestillingsnummer og tidspunkt for ubetaling=$tidspunktForUtbetaling for publisering på kafka")

        val headers = tidspunktForUtbetaling?.let {
            RecordHeaders().add(
                KAFKA_CONSUMER_RECORD_PROCESSOR_SCHEDULED_AT,
                it.toString().toByteArray(),
            )
        }
        val record = StoredProducerRecord(
            config.bestillingTopic,
            bestillingsnummer.toByteArray(),
            Json.encodeToString(message).toByteArray(),
            KafkaUtils.headersToJson(headers),
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
                    UtbetalingStatusType.DELVIS_UTBETALT,
                    UtbetalingStatusType.UTBETALT,
                    UtbetalingStatusType.AVBRUTT,
                    -> false
                }
            },
            UtbetalingHandling.SLETT.takeIf {
                when (utbetaling.status) {
                    UtbetalingStatusType.RETURNERT,
                    UtbetalingStatusType.INNSENDT,
                    -> UtbetalingType.from(utbetaling) == UtbetalingType.KORRIGERING

                    UtbetalingStatusType.FERDIG_BEHANDLET,
                    UtbetalingStatusType.GENERERT,
                    UtbetalingStatusType.TIL_ATTESTERING,
                    UtbetalingStatusType.DELVIS_UTBETALT,
                    UtbetalingStatusType.UTBETALT,
                    UtbetalingStatusType.AVBRUTT,
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
                        behandletAv = opprettelse.behandletAv,
                    )
                }
                .toSet()
        }

        fun tilgangTilHandling(handling: UtbetalingHandling, ansatt: NavAnsatt): Boolean {
            return when (handling) {
                UtbetalingHandling.SEND_TIL_ATTESTERING -> ansatt.hasGenerellRolle(Rolle.SAKSBEHANDLER_OKONOMI)
                UtbetalingHandling.SLETT -> ansatt.hasGenerellRolle(Rolle.SAKSBEHANDLER_OKONOMI)
            }
        }

        fun tilgangTilHandling(
            handling: UtbetalingLinjeHandling,
            ansatt: NavAnsatt,
            kostnadssted: NavEnhetNummer,
            behandletAv: Agent,
        ): Boolean {
            val erBeslutter = ansatt.hasKontorspesifikkRolle(
                Rolle.ATTESTANT_UTBETALING,
                setOf(kostnadssted),
            )
            val erSaksbehandler = ansatt.hasGenerellRolle(Rolle.SAKSBEHANDLER_OKONOMI)

            return when (handling) {
                UtbetalingLinjeHandling.ATTESTER ->
                    erBeslutter && behandletAv != ansatt.navIdent

                UtbetalingLinjeHandling.RETURNER -> erBeslutter

                UtbetalingLinjeHandling.SEND_TIL_ATTESTERING -> erSaksbehandler
            }
        }
    }
}
