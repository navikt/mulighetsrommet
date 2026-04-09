package no.nav.mulighetsrommet.api.utbetaling.service

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.flatMap
import arrow.core.getOrElse
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
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.arrangor.model.Betalingsinformasjon
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsatt
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.TilsagnService
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.api.totrinnskontroll.db.TotrinnskontrollDbo
import no.nav.mulighetsrommet.api.totrinnskontroll.db.toDbo
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Besluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.utbetaling.api.OpprettUtbetalingLinjerRequest
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingHandling
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingLinjeHandling
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingType
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingLinjeDbo
import no.nav.mulighetsrommet.api.utbetaling.model.AutomatiskUtbetalingResult
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakerAdvarsel
import no.nav.mulighetsrommet.api.utbetaling.model.UpsertUtbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingAdvarsler
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFastSatsPerTiltaksplassPerManed
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerHeleUkesverk
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerManedsverk
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerTimeOppfolging
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerUkesverk
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingLinje
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingLinjeReturnertAarsak
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingLinjeStatus
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.api.utbetaling.task.JournalforUtbetaling
import no.nav.mulighetsrommet.api.validation.Validated
import no.nav.mulighetsrommet.api.validation.validation
import no.nav.mulighetsrommet.clamav.Vedlegg
import no.nav.mulighetsrommet.kafka.KAFKA_CONSUMER_RECORD_PROCESSOR_SCHEDULED_AT
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.Arrangor
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Tiltaksadministrasjon
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.tiltak.okonomi.FakturaStatusType
import no.nav.tiltak.okonomi.OkonomiBestillingMelding
import no.nav.tiltak.okonomi.OpprettFaktura
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
    private val arrangorService: ArrangorService,
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
    ): Either<List<FieldError>, AutomatiskUtbetalingResult> {
        db.transaction {
            val utbetaling = queries.utbetaling.getAndAquireLock(utbetalingId)
            if (utbetaling.status != UtbetalingStatusType.GENERERT) {
                return FieldError.of("Utbetaling er allerede godkjent").nel().left()
            }

            queries.utbetaling.setInnsendtAvArrangor(utbetalingId, LocalDateTime.now())
            queries.utbetaling.setKid(utbetalingId, kid)
            queries.utbetaling.setStatus(utbetalingId, UtbetalingStatusType.TIL_BEHANDLING)

            scheduleJournalforUtbetaling(utbetalingId, vedlegg = emptyList())

            logEndring("Utbetaling sendt inn", utbetalingId, Arrangor)
        }

        return tryAutomatiskUtbetaling(utbetalingId).right()
    }

    suspend fun opprettUtbetaling(
        opprett: UpsertUtbetaling,
        agent: Agent,
    ): Either<List<FieldError>, Utbetaling> = db.transaction {
        if (queries.utbetaling.get(opprett.id) != null) {
            return FieldError.of("Utbetalingen er allerede opprettet").nel().left()
        }

        return upsert(opprett, agent).map {
            val operation = when (agent) {
                is Arrangor -> "Utbetaling sendt inn"
                else -> "Utbetaling opprettet"
            }
            logEndring(operation, opprett.id, agent)
        }
    }

    suspend fun redigerUtbetaling(
        rediger: UpsertUtbetaling,
        agent: Agent,
    ): Validated<Utbetaling> = db.transaction {
        val utbetaling = queries.utbetaling.getAndAquireLock(rediger.id)

        if (!kanRedigeres(utbetaling)) {
            return FieldError.of("Utbetalingen kan ikke redigeres").nel().left()
        }

        return upsert(rediger, agent).map {
            logEndring("Utbetaling redigert", it.id, agent)
        }
    }

    fun opprettUtbetalingLinjer(
        request: OpprettUtbetalingLinjerRequest,
        navIdent: NavIdent,
    ): Either<List<FieldError>, Utbetaling> = db.transaction {
        val utbetaling = queries.utbetaling.getAndAquireLock(request.utbetalingId)

        val utbetalingLinjeTilsagn = request.utbetalingLinjer.associate { req ->
            val tilsagn = queries.tilsagn.getOrError(req.tilsagnId)
            req.id to tilsagn
        }

        UtbetalingValidator
            .validateOpprettUtbetalingLinjer(
                UtbetalingValidator.OpprettUtbetalingLinjerCtx(
                    utbetaling = utbetaling,
                    linjer = request.utbetalingLinjer.map { req ->
                        val tilsagn = requireNotNull(utbetalingLinjeTilsagn[req.id])
                        UtbetalingValidator.OpprettUtbetalingLinjerCtx.Linje(
                            request = req,
                            tilsagn = UtbetalingValidator.OpprettUtbetalingLinjerCtx.Tilsagn(
                                status = tilsagn.status,
                                gjenstaendeBelop = tilsagn.gjenstaendeBelop(),
                            ),
                        )
                    },
                    begrunnelse = request.begrunnelseMindreBetalt,
                ),
            )
            .map { linje ->
                // Slett de som ikke er med i requesten
                queries.utbetalingLinje.getByUtbetalingId(utbetaling.id)
                    .filter { linje -> linje.id !in request.utbetalingLinjer.map { it.id } }
                    .forEach { linje ->
                        require(linje.status == UtbetalingLinjeStatus.RETURNERT) {
                            "Fatal! UtbetalingLinje kan ikke slettes fordi den har status: ${linje.status}"
                        }
                        queries.utbetalingLinje.delete(linje.id)
                    }

                linje.forEach {
                    upsertUtbetalingLinje(
                        utbetaling,
                        requireNotNull(utbetalingLinjeTilsagn[it.id]),
                        it.id,
                        requireNotNull(it.pris),
                        it.gjorOppTilsagn,
                        navIdent,
                    )
                }
                queries.utbetaling.setStatus(utbetaling.id, UtbetalingStatusType.TIL_ATTESTERING)
                queries.utbetaling.setBegrunnelseMindreBetalt(utbetaling.id, request.begrunnelseMindreBetalt)

                logEndring("Utbetaling sendt til attestering", utbetaling.id, navIdent)
            }
    }

    fun godkjennUtbetalingLinje(
        id: UUID,
        navIdent: NavIdent,
    ): Either<List<FieldError>, Utbetaling> = db.transaction {
        validateAccessAndLockUtbetaling(id, navIdent).flatMap { (utbetaling, linje) ->
            godkjennUtbetalingLinje(utbetaling, linje, navIdent)
        }
    }

    fun returnerUtbetalingLinje(
        id: UUID,
        aarsaker: List<UtbetalingLinjeReturnertAarsak>,
        forklaring: String?,
        navIdent: NavIdent,
    ): Either<List<FieldError>, Utbetaling> = db.transaction {
        validateAccessAndLockUtbetaling(id, navIdent).map { (_, linje) ->
            returnerUtbetalingLinje(linje, aarsaker, forklaring, navIdent)
        }
    }

    private fun QueryContext.validateAccessAndLockUtbetaling(id: UUID, navIdent: NavIdent) = validation {
        val linje = queries.utbetalingLinje.getOrError(id)
        val utbetaling = queries.utbetaling.getAndAquireLock(linje.utbetalingId)
        validate(utbetaling.status == UtbetalingStatusType.TIL_ATTESTERING && linje.status == UtbetalingLinjeStatus.TIL_ATTESTERING) {
            FieldError.of("Utbetaling er ikke satt til attestering")
        }

        val kostnadssted = queries.tilsagn.getOrError(linje.tilsagnId).kostnadssted
        val ansatt = queries.ansatt.getByNavIdentOrError(navIdent)
        validate(ansatt.hasKontorspesifikkRolle(Rolle.ATTESTANT_UTBETALING, setOf(kostnadssted.enhetsnummer))) {
            FieldError.of("Kan ikke attestere utbetalingen fordi du ikke er attestant ved tilsagnets kostnadssted (${kostnadssted.navn})")
        }

        Pair(utbetaling, linje)
    }

    fun slettKorreksjon(id: UUID): Either<List<FieldError>, Unit> = db.transaction {
        val utbetaling = queries.utbetaling.getAndAquireLock(id)
        when (utbetaling.status) {
            UtbetalingStatusType.RETURNERT,
            UtbetalingStatusType.TIL_BEHANDLING,
            -> Unit

            UtbetalingStatusType.GENERERT,
            UtbetalingStatusType.TIL_ATTESTERING,
            UtbetalingStatusType.FERDIG_BEHANDLET,
            UtbetalingStatusType.DELVIS_UTBETALT,
            UtbetalingStatusType.UTBETALT,
            UtbetalingStatusType.AVBRUTT,
            -> return FieldError.root("Kan ikke slette utbetaling fordi den har status: ${utbetaling.status}")
                .nel()
                .left()
        }
        if (UtbetalingType.from(utbetaling) != UtbetalingType.KORRIGERING) {
            return FieldError.root("Kan kun slette korreksjoner").nel().left()
        }
        queries.utbetalingLinje.getByUtbetalingId(id).forEach { linje ->
            if (linje.status != UtbetalingLinjeStatus.RETURNERT) {
                return FieldError.root("UtbetalingLinje var i feil status").nel().left()
            }
            queries.utbetalingLinje.delete(linje.id)
        }

        queries.utbetaling.delete(id).right()
    }

    fun avbrytUtbetaling(utbetalingId: UUID, begrunnelse: String, agent: Agent): Either<List<FieldError>, Utbetaling> = db.transaction {
        val utbetaling = queries.utbetaling.getAndAquireLock(utbetalingId)
        when (utbetaling.status) {
            UtbetalingStatusType.GENERERT,
            UtbetalingStatusType.DELVIS_UTBETALT,
            UtbetalingStatusType.TIL_ATTESTERING,
            UtbetalingStatusType.FERDIG_BEHANDLET,
            UtbetalingStatusType.UTBETALT,
            UtbetalingStatusType.AVBRUTT,
            -> return FieldError.root("Utbetalingen kan ikke avbrytes").nel().left()

            UtbetalingStatusType.TIL_BEHANDLING,
            UtbetalingStatusType.RETURNERT,
            -> Unit
        }

        queries.utbetaling.avbrytUtbetaling(utbetalingId, begrunnelse, Instant.now())

        logEndring("Utbetaling avbrutt", utbetaling.id, agent).right()
    }

    fun republishFaktura(fakturanummer: String): UtbetalingLinje = db.transaction {
        queries.utbetalingLinje.getOrError(fakturanummer).also { publishOpprettFaktura(it) }
    }

    fun oppdaterFakturaStatus(
        fakturanummer: String,
        nyStatus: FakturaStatusType,
        fakturaStatusEndretTidspunkt: LocalDateTime,
    ): Utbetaling = db.transaction {
        val originalUtbetalingLinje = queries.utbetalingLinje.getOrError(fakturanummer)
        if (originalUtbetalingLinje.faktura.statusEndretTidspunkt != null &&
            originalUtbetalingLinje.faktura.statusEndretTidspunkt > fakturaStatusEndretTidspunkt
        ) {
            return getOrError(originalUtbetalingLinje.utbetalingId)
        }

        queries.utbetalingLinje.setFakturaStatus(fakturanummer, nyStatus, fakturaStatusEndretTidspunkt)

        when (nyStatus) {
            FakturaStatusType.FEILET,
            FakturaStatusType.SENDT,
            FakturaStatusType.IKKE_BETALT,
            -> {
                check(!originalUtbetalingLinje.faktura.erUtbetalt()) {
                    "UtbetalingLinje ${originalUtbetalingLinje.id} faktura status er ${originalUtbetalingLinje.faktura.status}, ny status $nyStatus"
                }
                queries.utbetalingLinje.setStatus(fakturanummer, UtbetalingLinjeStatus.OVERFORT_TIL_UTBETALING)
                getOrError(originalUtbetalingLinje.utbetalingId)
            }

            FakturaStatusType.DELVIS_BETALT,
            FakturaStatusType.FULLT_BETALT,
            -> {
                queries.utbetalingLinje.setStatus(fakturanummer, UtbetalingLinjeStatus.UTBETALT)
                oppdaterUtbetalingForUtbetaltUtbetalingLinje(originalUtbetalingLinje.utbetalingId)
                if (!originalUtbetalingLinje.faktura.erUtbetalt()) {
                    logUtbetalingLinjeUtbetalt(originalUtbetalingLinje, fakturaStatusEndretTidspunkt)
                } else {
                    getOrError(originalUtbetalingLinje.utbetalingId)
                }
            }
        }
    }

    private suspend fun TransactionalQueryContext.upsert(
        upsert: UpsertUtbetaling,
        agent: Agent,
    ): Either<NonEmptyList<FieldError>, UtbetalingDbo> = when (upsert) {
        is UpsertUtbetaling.Anskaffelse -> upsertAnskaffelse(upsert, agent)
        is UpsertUtbetaling.Korreksjon -> upsertKorreksjon(upsert)
    }

    private suspend fun TransactionalQueryContext.upsertAnskaffelse(
        upsert: UpsertUtbetaling.Anskaffelse,
        agent: Agent,
    ): Either<NonEmptyList<FieldError>, UtbetalingDbo> {
        val gjennomforing = queries.gjennomforing.getGjennomforingTiltaksadministrasjon(upsert.gjennomforingId)

        val dbo = UtbetalingDbo(
            id = upsert.id,
            gjennomforingId = upsert.gjennomforingId,
            status = UtbetalingStatusType.TIL_BEHANDLING,
            valuta = upsert.beregning.output.pris.valuta,
            beregning = upsert.beregning,
            periode = upsert.periode,
            kommentar = upsert.kommentar,
            korreksjonGjelderUtbetalingId = null,
            korreksjonBegrunnelse = null,
            tilskuddstype = upsert.tilskuddstype,
            journalpostId = upsert.journalpostId,
            innsendtAvArrangorTidspunkt = when (agent) {
                is Arrangor -> LocalDateTime.now()
                else -> null
            },
            betalingsinformasjon = getUtbetalingsinformasjon(gjennomforing.arrangor.id, upsert.kid),
            utbetalesTidligstTidspunkt = config.tidligstTidspunktForUtbetaling.calculate(
                gjennomforing.tiltakstype.tiltakskode,
                upsert.periode,
            ),
            blokkeringer = emptySet(),
        )

        queries.utbetaling.upsert(dbo)

        if (agent is Arrangor) {
            scheduleJournalforUtbetaling(dbo.id, upsert.vedlegg)
        }

        return dbo.right()
    }

    private suspend fun TransactionalQueryContext.upsertKorreksjon(
        upsert: UpsertUtbetaling.Korreksjon,
    ): Either<NonEmptyList<FieldError>, UtbetalingDbo> {
        val utbetaling = queries.utbetaling.get(upsert.korreksjonGjelderUtbetalingId)
            ?: return FieldError.of("Utbetaling som skal korrigeres eksisterer ikke").nel().left()

        when (utbetaling.status) {
            UtbetalingStatusType.RETURNERT,
            UtbetalingStatusType.TIL_BEHANDLING,
            UtbetalingStatusType.GENERERT,
            UtbetalingStatusType.TIL_ATTESTERING,
            UtbetalingStatusType.AVBRUTT,
            -> return FieldError.of("Utbetaling kan ikke korrigeres når den har status ${utbetaling.status}")
                .nel()
                .left()

            UtbetalingStatusType.FERDIG_BEHANDLET,
            UtbetalingStatusType.DELVIS_UTBETALT,
            UtbetalingStatusType.UTBETALT,
            -> Unit
        }

        val gjennomforing = queries.gjennomforing.getGjennomforingTiltaksadministrasjon(utbetaling.gjennomforing.id)

        val dbo = UtbetalingDbo(
            id = upsert.id,
            gjennomforingId = gjennomforing.id,
            status = UtbetalingStatusType.TIL_BEHANDLING,
            valuta = upsert.beregning.output.pris.valuta,
            beregning = upsert.beregning,
            periode = upsert.periode,
            kommentar = upsert.kommentar,
            korreksjonGjelderUtbetalingId = upsert.korreksjonGjelderUtbetalingId,
            korreksjonBegrunnelse = upsert.korreksjonBegrunnelse,
            tilskuddstype = upsert.tilskuddstype,
            journalpostId = null,
            innsendtAvArrangorTidspunkt = null,
            betalingsinformasjon = getUtbetalingsinformasjon(gjennomforing.arrangor.id, upsert.kid),
            utbetalesTidligstTidspunkt = config.tidligstTidspunktForUtbetaling.calculate(
                gjennomforing.tiltakstype.tiltakskode,
                upsert.periode,
            ),
            blokkeringer = emptySet(),
        )

        queries.utbetaling.upsert(dbo)

        return dbo.right()
    }

    private suspend fun getUtbetalingsinformasjon(arrangorId: UUID, kid: Kid?): Betalingsinformasjon {
        return when (val betalingsinformasjon = arrangorService.getBetalingsinformasjon(arrangorId)) {
            is Betalingsinformasjon.BBan -> Betalingsinformasjon.BBan(betalingsinformasjon.kontonummer, kid)
            is Betalingsinformasjon.IBan -> betalingsinformasjon
        }
    }

    private fun TransactionalQueryContext.logUtbetalingLinjeUtbetalt(
        utbetalingLinje: UtbetalingLinje,
        fakturaStatusEndretTidspunkt: LocalDateTime,
    ): Utbetaling {
        val tilsagn = queries.tilsagn.getOrError(utbetalingLinje.tilsagnId)
        return logEndring(
            "Betaling for tilsagn ${tilsagn.bestilling.bestillingsnummer} er utbetalt",
            utbetalingLinje.utbetalingId,
            Tiltaksadministrasjon,
            timestamp = fakturaStatusEndretTidspunkt,
        )
    }

    private fun TransactionalQueryContext.oppdaterUtbetalingForUtbetaltUtbetalingLinje(
        utbetalingId: UUID,
    ) {
        val utbetaling = queries.utbetaling.getAndAquireLock(utbetalingId)
        val utbetalingLinjer = queries.utbetalingLinje.getByUtbetalingId(utbetaling.id)

        val oppdatertUtbetalingStatus = when {
            utbetalingLinjer.all { it.status == UtbetalingLinjeStatus.UTBETALT } -> UtbetalingStatusType.UTBETALT
            utbetalingLinjer.any { it.status == UtbetalingLinjeStatus.UTBETALT } -> UtbetalingStatusType.DELVIS_UTBETALT
            else -> utbetaling.status
        }
        if (utbetaling.status != oppdatertUtbetalingStatus) {
            queries.utbetaling.setStatus(utbetaling.id, oppdatertUtbetalingStatus)
        }
    }

    private fun tryAutomatiskUtbetaling(utbetalingId: UUID): AutomatiskUtbetalingResult {
        return try {
            automatiskUtbetaling(utbetalingId).also { result ->
                log.info("Automatisk utbetaling for utbetaling=$utbetalingId resulterte i: $result")
            }
        } catch (error: AttesterUtbetalingException) {
            log.error("Uventet valideringsfeil oppsto under automatisk utbetaling: ${error.errors}")
            AutomatiskUtbetalingResult.VALIDERINGSFEIL
        }
    }

    private fun automatiskUtbetaling(utbetalingId: UUID): AutomatiskUtbetalingResult = db.transaction {
        val utbetaling = queries.utbetaling.getAndAquireLock(utbetalingId)

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

        val tilsagn = queries.tilsagn.getAndAquireLock(relevanteTilsagn[0].id)
        if (tilsagn.gjenstaendeBelop() < utbetaling.beregning.output.pris) {
            return AutomatiskUtbetalingResult.IKKE_NOK_PENGER
        }

        val utbetalingLinjer = queries.utbetalingLinje.getByUtbetalingId(utbetalingId)
        if (utbetalingLinjer.isNotEmpty()) {
            return AutomatiskUtbetalingResult.UTBETALINGLINJER_ALLEREDE_OPPRETTET
        }

        val id = UUID.randomUUID()
        upsertUtbetalingLinje(
            utbetaling = utbetaling,
            tilsagn = tilsagn,
            id = id,
            pris = utbetaling.beregning.output.pris,
            gjorOppTilsagn = tilsagn.periode.getLastInclusiveDate() in utbetaling.periode,
            behandletAv = Tiltaksadministrasjon,
        )

        val linje = queries.utbetalingLinje.getOrError(id)
        godkjennUtbetalingLinje(utbetaling, linje, Tiltaksadministrasjon)
            .map { AutomatiskUtbetalingResult.GODKJENT }
            .getOrElse { throw AttesterUtbetalingException(it) }
    }

    private fun TransactionalQueryContext.upsertUtbetalingLinje(
        utbetaling: Utbetaling,
        tilsagn: Tilsagn,
        id: UUID,
        pris: ValutaBelop,
        gjorOppTilsagn: Boolean,
        behandletAv: Agent,
    ) {
        require(tilsagn.status == TilsagnStatus.GODKJENT) {
            "Tilsagn er ikke godkjent id=${tilsagn.id} status=${tilsagn.status}"
        }

        val periode = requireNotNull(utbetaling.periode.intersect(tilsagn.periode)) {
            "Utbetalingsperiode og tilsagnsperiode overlapper ikke"
        }

        val linje = queries.utbetalingLinje.get(id)

        val lopenummer = linje?.lopenummer
            ?: queries.utbetalingLinje.getNextLopenummerByTilsagn(tilsagn.id)

        val fakturanummer = linje?.faktura?.fakturanummer
            ?: "${tilsagn.bestilling.bestillingsnummer}-$lopenummer"

        val dbo = UtbetalingLinjeDbo(
            id = id,
            utbetalingId = utbetaling.id,
            tilsagnId = tilsagn.id,
            status = UtbetalingLinjeStatus.TIL_ATTESTERING,
            periode = periode,
            pris = pris,
            gjorOppTilsagn = gjorOppTilsagn,
            lopenummer = lopenummer,
            fakturanummer = fakturanummer,
            fakturaStatusEndretTidspunkt = null,
            fakturaStatus = null,
        )

        queries.utbetalingLinje.upsert(dbo)

        val opprettelse = TotrinnskontrollDbo(
            id = UUID.randomUUID(),
            entityId = id,
            type = Totrinnskontroll.Type.OPPRETT,
            behandletAv = behandletAv,
            behandletTidspunkt = LocalDateTime.now(),
            besluttetAv = null,
            besluttetTidspunkt = null,
            besluttelse = null,
            aarsaker = emptyList(),
            forklaring = null,
        )
        queries.totrinnskontroll.upsert(opprettelse)
    }

    private fun TransactionalQueryContext.godkjennUtbetalingLinje(
        utbetaling: Utbetaling,
        utbetalingLinje: UtbetalingLinje,
        besluttetAv: Agent,
    ): Either<List<FieldError>, Utbetaling> {
        val opprettelse = queries.totrinnskontroll.getOrError(utbetalingLinje.id, Totrinnskontroll.Type.OPPRETT)
        require(opprettelse.besluttetAv == null) {
            "Utbetaling er allerede besluttet"
        }

        if (besluttetAv is NavIdent && opprettelse.behandletAv is NavIdent && besluttetAv == opprettelse.behandletAv) {
            return listOf(FieldError.of("Kan ikke attestere en utbetaling du selv har opprettet")).left()
        }

        queries.utbetalingLinje.setStatus(utbetalingLinje.id, UtbetalingLinjeStatus.GODKJENT)
        val godkjentOpprettelse = opprettelse.copy(
            besluttetAv = besluttetAv,
            besluttelse = Besluttelse.GODKJENT,
            besluttetTidspunkt = LocalDateTime.now(),
            aarsaker = emptyList(),
            forklaring = null,
        )
        queries.totrinnskontroll.upsert(godkjentOpprettelse.toDbo())

        val linjer = queries.utbetalingLinje.getByUtbetalingId(utbetalingLinje.utbetalingId)
            .associateWith { linje ->
                val tilsagn = queries.tilsagn.getAndAquireLock(linje.tilsagnId)
                if (tilsagn.status != TilsagnStatus.GODKJENT) {
                    return returnerUtbetalingLinje(
                        linje,
                        listOf(UtbetalingLinjeReturnertAarsak.TILSAGN_FEIL_STATUS),
                        null,
                        Tiltaksadministrasjon,
                    ).right()
                }
                tilsagn
            }

        return if (linjer.all { it.key.status == UtbetalingLinjeStatus.GODKJENT }) {
            godkjennUtbetaling(utbetalingLinje.utbetalingId, linjer)
        } else {
            utbetaling
        }.right()
    }

    private fun TransactionalQueryContext.godkjennUtbetaling(
        id: UUID,
        linjer: Map<UtbetalingLinje, Tilsagn>,
    ): Utbetaling {
        linjer.forEach { (linje, tilsagn) ->
            queries.utbetalingLinje.setStatus(linje.id, UtbetalingLinjeStatus.OVERFORT_TIL_UTBETALING)

            val benyttetBelop = tilsagn.belopBrukt + linje.pris
            queries.tilsagn.setBruktBelop(tilsagn.id, benyttetBelop)

            if (linje.gjorOppTilsagn || benyttetBelop == tilsagn.beregning.output.pris) {
                gjorOppTilsagnForUtbetalingLinje(linje.id, tilsagn)
            }
            publishOpprettFaktura(linje)
        }

        queries.utbetaling.setStatus(id, UtbetalingStatusType.FERDIG_BEHANDLET)
        return logEndring("Overført til utbetaling", id, Tiltaksadministrasjon)
    }

    private fun TransactionalQueryContext.gjorOppTilsagnForUtbetalingLinje(utbetalingLinjeId: UUID, tilsagn: Tilsagn) {
        val opprettelse = queries.totrinnskontroll.getOrError(utbetalingLinjeId, Totrinnskontroll.Type.OPPRETT)
        val tilsagnTilOppgjor = tilsagnService.setTilOppgjor(
            tilsagn,
            opprettelse.behandletAv,
            aarsaker = listOf(),
            forklaring = null,
            operation = "Sendt til oppgjør ved behandling av utbetaling",
        )
        tilsagnService.gjorOppTilsagn(
            tilsagnTilOppgjor,
            requireNotNull(opprettelse.besluttetAv),
            operation = "Tilsagn oppgjort ved attestering av utbetaling",
        ).onLeft { errors ->
            throw AttesterUtbetalingException(errors)
        }
    }

    private fun TransactionalQueryContext.returnerUtbetalingLinje(
        linje: UtbetalingLinje,
        aarsaker: List<UtbetalingLinjeReturnertAarsak>,
        forklaring: String?,
        besluttetAv: Agent,
    ): Utbetaling {
        setReturnertUtbetalingLinje(linje, aarsaker, forklaring, besluttetAv)

        // Set også de resterende utbetalingslinjene som returnert
        queries.utbetalingLinje.getByUtbetalingId(linje.utbetalingId)
            .filter { it.id != linje.id }
            .forEach {
                setReturnertUtbetalingLinje(
                    it,
                    listOf(UtbetalingLinjeReturnertAarsak.PROPAGERT_RETUR),
                    null,
                    Tiltaksadministrasjon,
                )
            }

        queries.utbetaling.setStatus(linje.utbetalingId, UtbetalingStatusType.RETURNERT)
        return logEndring("Utbetaling returnert", linje.utbetalingId, besluttetAv)
    }

    private fun TransactionalQueryContext.setReturnertUtbetalingLinje(
        utbetalingLinje: UtbetalingLinje,
        aarsaker: List<UtbetalingLinjeReturnertAarsak>,
        forklaring: String?,
        besluttetAv: Agent,
    ) {
        queries.utbetalingLinje.setStatus(utbetalingLinje.id, UtbetalingLinjeStatus.RETURNERT)
        val opprettelse = queries.totrinnskontroll.getOrError(utbetalingLinje.id, Totrinnskontroll.Type.OPPRETT)
        val avvistOpprettelse = opprettelse.copy(
            besluttetAv = besluttetAv,
            besluttelse = Besluttelse.AVVIST,
            aarsaker = aarsaker.map { it.name },
            forklaring = forklaring,
            besluttetTidspunkt = LocalDateTime.now(),
        )
        queries.totrinnskontroll.upsert(avvistOpprettelse.toDbo())
    }

    private fun TransactionalQueryContext.logEndring(
        operation: String,
        utbetalingId: UUID,
        endretAv: Agent,
        timestamp: LocalDateTime = LocalDateTime.now(),
    ): Utbetaling {
        val utbetaling = getOrError(utbetalingId)
        queries.endringshistorikk.logEndring(
            DocumentClass.UTBETALING,
            operation,
            endretAv,
            utbetalingId,
            timestamp,
        ) {
            Json.encodeToJsonElement(utbetaling)
        }
        return utbetaling
    }

    private fun TransactionalQueryContext.scheduleJournalforUtbetaling(utbetalingId: UUID, vedlegg: List<Vedlegg>) {
        journalforUtbetaling.schedule(
            utbetalingId = utbetalingId,
            startTime = Instant.now(),
            tx = session,
            vedlegg = vedlegg,
        )
    }

    private fun TransactionalQueryContext.publishOpprettFaktura(linje: UtbetalingLinje) {
        val opprettelse = queries.totrinnskontroll.getOrError(linje.id, Totrinnskontroll.Type.OPPRETT)
        check(opprettelse.besluttetAv != null && opprettelse.besluttetTidspunkt != null && opprettelse.besluttelse == Besluttelse.GODKJENT) {
            "UtbetalingLinje id=${linje.id} må være besluttet godkjent for å sendes til økonomi"
        }

        val utbetaling = queries.utbetaling.getOrError(linje.utbetalingId)
        val tilsagn = queries.tilsagn.getOrError(linje.tilsagnId)

        val beskrivelse = """
            Tiltakstype: ${tilsagn.tiltakstype.navn}
            Periode: ${tilsagn.periode.formatPeriode()}
            Tilsagnsnummer: ${tilsagn.bestilling.bestillingsnummer}
        """.trimIndent()

        val betalingsinformasjon = when (utbetaling.betalingsinformasjon) {
            is Betalingsinformasjon.BBan ->
                OpprettFaktura.Betalingsinformasjon.BBan(
                    kontonummer = utbetaling.betalingsinformasjon.kontonummer,
                    kid = utbetaling.betalingsinformasjon.kid,
                )

            is Betalingsinformasjon.IBan ->
                OpprettFaktura.Betalingsinformasjon.IBan(
                    iban = utbetaling.betalingsinformasjon.iban,
                    bic = utbetaling.betalingsinformasjon.bic,
                    bankLandKode = utbetaling.betalingsinformasjon.bankLandKode,
                    bankNavn = utbetaling.betalingsinformasjon.bankNavn,
                )

            null -> {
                throw IllegalStateException(
                    "Bankkonto informasjon mangler for utbetaling med id=${linje.utbetalingId}",
                )
            }
        }

        queries.utbetalingLinje.setFakturaSendtTidspunk(linje.id, LocalDateTime.now())

        val faktura = OpprettFaktura(
            fakturanummer = linje.faktura.fakturanummer,
            bestillingsnummer = tilsagn.bestilling.bestillingsnummer,
            betalingsinformasjon = betalingsinformasjon,
            periode = linje.periode,
            behandletAv = opprettelse.behandletAv.toOkonomiPart(),
            behandletTidspunkt = opprettelse.behandletTidspunkt,
            besluttetAv = opprettelse.besluttetAv.toOkonomiPart(),
            besluttetTidspunkt = opprettelse.besluttetTidspunkt,
            gjorOppBestilling = linje.gjorOppTilsagn,
            beskrivelse = beskrivelse,
            belop = linje.pris.belop,
            valuta = linje.pris.valuta,
        )

        val tidspunktForUtbetaling = linje.faktura.utbetalesTidligstTidspunkt
            ?: config.tidligstTidspunktForUtbetaling.calculate(tilsagn.tiltakstype.tiltakskode, faktura.periode)
        val message = OkonomiBestillingMelding.Faktura(faktura)
        storeOkonomiMelding(faktura.bestillingsnummer, message, tidspunktForUtbetaling)
    }

    private fun TransactionalQueryContext.storeOkonomiMelding(
        bestillingsnummer: String,
        message: OkonomiBestillingMelding,
        tidspunktForUtbetaling: Instant?,
    ) {
        log.info("Lagrer faktura for utbetalingslinje med bestillingsnummer=$bestillingsnummer og tidspunkt for ubetaling=$tidspunktForUtbetaling for publisering på kafka")

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

    fun getAdvarsler(utbetaling: Utbetaling): List<DeltakerAdvarsel> = db.session {
        return when (utbetaling.status) {
            UtbetalingStatusType.GENERERT -> {
                val forslag = queries.deltakerForslag.getForslagByGjennomforing(utbetaling.gjennomforing.id)
                val deltakere = queries.deltaker
                    .getByGjennomforingId(utbetaling.gjennomforing.id)
                    .filter { it.id in utbetaling.beregning.input.deltakelser().map { it.deltakelseId } }

                UtbetalingAdvarsler.getAdvarsler(utbetaling, deltakere, forslag)
            }

            UtbetalingStatusType.TIL_BEHANDLING,
            UtbetalingStatusType.TIL_ATTESTERING,
            UtbetalingStatusType.RETURNERT,
            UtbetalingStatusType.FERDIG_BEHANDLET,
            UtbetalingStatusType.DELVIS_UTBETALT,
            UtbetalingStatusType.UTBETALT,
            UtbetalingStatusType.AVBRUTT,
            -> emptyList()
        }
    }

    companion object {
        fun utbetalingHandlinger(utbetaling: Utbetaling, ansatt: NavAnsatt) = setOfNotNull(
            UtbetalingHandling.SEND_TIL_ATTESTERING.takeIf {
                when (utbetaling.status) {
                    UtbetalingStatusType.TIL_BEHANDLING,
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
                    UtbetalingStatusType.TIL_BEHANDLING,
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
            UtbetalingHandling.OPPRETT_KORREKSJON.takeIf {
                utbetaling.korreksjon == null && when (utbetaling.status) {
                    UtbetalingStatusType.RETURNERT,
                    UtbetalingStatusType.TIL_BEHANDLING,
                    UtbetalingStatusType.GENERERT,
                    UtbetalingStatusType.TIL_ATTESTERING,
                    UtbetalingStatusType.AVBRUTT,
                    -> false

                    UtbetalingStatusType.FERDIG_BEHANDLET,
                    UtbetalingStatusType.DELVIS_UTBETALT,
                    UtbetalingStatusType.UTBETALT,
                    -> true
                }
            },
            UtbetalingHandling.REDIGER.takeIf { kanRedigeres(utbetaling) },
        )
            .filter {
                tilgangTilHandling(handling = it, ansatt = ansatt)
            }
            .toSet()

        fun linjeHandlinger(
            linje: UtbetalingLinje,
            opprettelse: Totrinnskontroll,
            kostnadssted: NavEnhetNummer,
            ansatt: NavAnsatt,
        ): Set<UtbetalingLinjeHandling> {
            return setOfNotNull(
                UtbetalingLinjeHandling.ATTESTER.takeIf { linje.status == UtbetalingLinjeStatus.TIL_ATTESTERING },
                UtbetalingLinjeHandling.RETURNER.takeIf { linje.status == UtbetalingLinjeStatus.TIL_ATTESTERING },
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
            val saksbehandlerOkonomi = ansatt.hasGenerellRolle(Rolle.SAKSBEHANDLER_OKONOMI)
            return when (handling) {
                UtbetalingHandling.OPPRETT_KORREKSJON -> saksbehandlerOkonomi
                UtbetalingHandling.REDIGER -> saksbehandlerOkonomi
                UtbetalingHandling.SEND_TIL_ATTESTERING -> saksbehandlerOkonomi
                UtbetalingHandling.SLETT -> saksbehandlerOkonomi
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

/**
 * Ved unntakstilfeller så kan attestering av utbetalinger feile pga. uventede valideringsfeil. Årsaker
 * kan f.eks. være samtidighetsproblemer, glemte preconditions/låser eller andre bugs/mangler i koden.
 *
 * Disse blir kastet som exceptions i stedet for returneres som en [Either.Left] fordi det integrerer bedre med
 * automatisk rollback av database-transaksjoner.
 */
private class AttesterUtbetalingException(val errors: List<FieldError>) : Exception()

fun kanRedigeres(utbetaling: Utbetaling): Boolean = utbetaling.innsending == null && when (utbetaling.status) {
    UtbetalingStatusType.GENERERT,
    UtbetalingStatusType.TIL_ATTESTERING,
    UtbetalingStatusType.FERDIG_BEHANDLET,
    UtbetalingStatusType.DELVIS_UTBETALT,
    UtbetalingStatusType.UTBETALT,
    UtbetalingStatusType.AVBRUTT,
    -> false

    UtbetalingStatusType.TIL_BEHANDLING,
    UtbetalingStatusType.RETURNERT,
    -> true
}
