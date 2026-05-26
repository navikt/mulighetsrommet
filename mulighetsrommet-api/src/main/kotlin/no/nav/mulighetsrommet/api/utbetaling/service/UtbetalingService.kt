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
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.TransactionalQueryContext
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.arrangor.model.Betalingsinformasjon
import no.nav.mulighetsrommet.api.endringshistorikk.EndringshistorikkType
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtale
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingEnkeltplass
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingTiltaksadministrasjon
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnDbo
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatusAarsak
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.api.tilsagn.model.UpsertTilsagn
import no.nav.mulighetsrommet.api.totrinnskontroll.TotrinnskontrollService
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollBesluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollType
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingLinjeDbo
import no.nav.mulighetsrommet.api.utbetaling.model.AutomatisertUtbetalingResult
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakerAdvarsel
import no.nav.mulighetsrommet.api.utbetaling.model.OpprettUtbetalingLinje
import no.nav.mulighetsrommet.api.utbetaling.model.UpsertUtbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingAdvarsler
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregning
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingException
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingLinje
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingLinjeReturnertAarsak
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingLinjeStatus
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.api.utils.DatoUtils.tilNorskLocalDateTime
import no.nav.mulighetsrommet.api.validation.Validated
import no.nav.mulighetsrommet.api.validation.validation
import no.nav.mulighetsrommet.kafka.KAFKA_CONSUMER_RECORD_PROCESSOR_SCHEDULED_AT
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.Arena
import no.nav.mulighetsrommet.model.Arrangor
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltaksadministrasjon
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.model.withValuta
import no.nav.tiltak.okonomi.AnnullerBestilling
import no.nav.tiltak.okonomi.FakturaStatusType
import no.nav.tiltak.okonomi.GjorOppBestilling
import no.nav.tiltak.okonomi.OkonomiBestillingMelding
import no.nav.tiltak.okonomi.OpprettBestilling
import no.nav.tiltak.okonomi.OpprettFaktura
import no.nav.tiltak.okonomi.Tilskuddstype
import no.nav.tiltak.okonomi.toOkonomiPart
import org.apache.kafka.common.header.internals.RecordHeaders
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

class UtbetalingService(
    private val config: Config,
    private val arrangorService: ArrangorService,
    private val totrinnskontroll: TotrinnskontrollService,
) {
    data class Config(
        val bestillingTopic: String,
        val tidligstTidspunktForUtbetaling: TidligstTidspunktForUtbetalingCalculator,
    )

    context(tx: TransactionalQueryContext)
    fun godkjentAvArrangor(
        utbetalingId: UUID,
        kid: Kid?,
    ): Either<List<FieldError>, Utbetaling> = with(tx) {
        val utbetaling = queries.utbetaling.getAndAquireLock(utbetalingId)
        if (utbetaling.status != UtbetalingStatusType.GENERERT) {
            return FieldError.of("Utbetaling er allerede godkjent").nel().left()
        }

        queries.utbetaling.setInnsendtAvArrangor(utbetalingId, LocalDateTime.now())
        queries.utbetaling.setKid(utbetalingId, kid)
        queries.utbetaling.setStatus(utbetalingId, UtbetalingStatusType.TIL_BEHANDLING)

        return logEndring("Utbetaling sendt inn", utbetalingId, Arrangor).right()
    }

    context(tx: TransactionalQueryContext)
    suspend fun opprettUtbetaling(
        opprett: UpsertUtbetaling,
        agent: Agent,
    ): Either<List<FieldError>, Utbetaling> = with(tx) {
        if (queries.utbetaling.get(opprett.id) != null) {
            return FieldError.of("Utbetalingen er allerede opprettet").nel().left()
        }

        return upsert(opprett).map {
            val operation = when (agent) {
                is Arrangor -> "Utbetaling sendt inn"
                else -> "Utbetaling opprettet"
            }
            logEndring(operation, opprett.id, agent)
        }
    }

    context(tx: TransactionalQueryContext)
    suspend fun redigerUtbetaling(
        rediger: UpsertUtbetaling,
        agent: Agent,
    ): Validated<Utbetaling> = with(tx) {
        val utbetaling = queries.utbetaling.getAndAquireLock(rediger.id)
        if (!utbetaling.erTilBehandling()) {
            return FieldError.of("Utbetalingen kan ikke redigeres").nel().left()
        }

        return upsert(rediger).map {
            logEndring("Utbetaling redigert", it.id, agent)
        }
    }

    context(tx: TransactionalQueryContext)
    fun oppdaterBeregning(
        id: UUID,
        beregning: UtbetalingBeregning,
        agent: Agent,
    ): Validated<Utbetaling> = with(tx) {
        val utbetaling = queries.utbetaling.getAndAquireLock(id)

        if (beregning == utbetaling.beregning) {
            return utbetaling.right()
        }

        queries.utbetaling.setBeregning(id, beregning)

        logEndring("Beregning oppdatert", id, agent).right()
    }

    context(tx: TransactionalQueryContext)
    fun sendTilAttestering(
        utbetalingId: UUID,
        linjer: List<OpprettUtbetalingLinje>,
        agent: Agent,
    ): Either<List<FieldError>, Utbetaling> = with(tx) {
        val utbetaling = queries.utbetaling.getAndAquireLock(utbetalingId)

        val linjerSomSkalSlettes = queries.utbetalingLinje.getByUtbetalingId(utbetaling.id).filter { linje ->
            linje.id !in linjer.map { it.id }
        }

        if (linjerSomSkalSlettes.any { it.status != UtbetalingLinjeStatus.RETURNERT }) {
            return FieldError.of("Utbetaling kan ikke sendes til attestering fordi den allerede har andre utbetalingslinjer")
                .nel()
                .left()
        }

        linjerSomSkalSlettes.forEach { linje ->
            queries.utbetalingLinje.delete(linje.id)
        }

        linjer.forEach { linje ->
            upsertUtbetalingLinje(
                id = linje.id,
                utbetaling = utbetaling,
                tilsagn = queries.tilsagn.getOrError(linje.tilsagnId),
                pris = linje.pris,
                gjorOppTilsagn = linje.gjorOppTilsagn,
                behandletAv = agent,
            )
        }
        queries.utbetaling.setStatus(utbetaling.id, UtbetalingStatusType.TIL_ATTESTERING)

        logEndring("Utbetaling sendt til attestering", utbetaling.id, agent).right()
    }

    context(tx: TransactionalQueryContext)
    fun godkjennUtbetalingLinje(
        id: UUID,
        agent: Agent,
    ): Either<List<FieldError>, Utbetaling> = with(tx) {
        validateAccessAndLockUtbetaling(id, agent).flatMap { (_, linje) ->
            godkjennUtbetalingLinje(linje, agent)
        }
    }

    context(tx: TransactionalQueryContext)
    fun returnerUtbetalingLinje(
        id: UUID,
        aarsaker: List<UtbetalingLinjeReturnertAarsak>,
        forklaring: String?,
        navIdent: NavIdent,
    ): Either<List<FieldError>, Utbetaling> = with(tx) {
        validateAccessAndLockUtbetaling(id, navIdent).map { (_, linje) ->
            returnerUtbetalingLinje(linje, aarsaker, forklaring, navIdent)
        }
    }

    context(tx: TransactionalQueryContext)
    fun slettKorreksjon(id: UUID): Either<List<FieldError>, Unit> = with(tx) {
        val utbetaling = queries.utbetaling.getAndAquireLock(id)
        if (!utbetaling.erTilBehandling()) {
            return FieldError.of("Kan ikke slette utbetaling fordi den har status: ${utbetaling.status}")
                .nel()
                .left()
        }
        if (!utbetaling.erKorreksjon()) {
            return FieldError.of("Kan kun slette korreksjoner").nel().left()
        }
        queries.utbetalingLinje.getByUtbetalingId(id).forEach { linje ->
            if (linje.status != UtbetalingLinjeStatus.RETURNERT) {
                return FieldError.of("UtbetalingLinje var i feil status").nel().left()
            }
            queries.utbetalingLinje.delete(linje.id)
        }

        queries.utbetaling.delete(id).right()
    }

    context(tx: TransactionalQueryContext)
    fun avbrytUtbetaling(
        utbetalingId: UUID,
        begrunnelse: String,
        agent: Agent,
    ): Either<List<FieldError>, Utbetaling> = with(tx) {
        val utbetaling = queries.utbetaling.getAndAquireLock(utbetalingId)
        if (!utbetaling.erTilBehandling()) {
            return FieldError.of("Utbetalingen kan ikke avbrytes").nel().left()
        }

        queries.utbetaling.avbrytUtbetaling(utbetalingId, begrunnelse, Instant.now())

        logEndring("Utbetaling avbrutt", utbetaling.id, agent).right()
    }

    context(tx: TransactionalQueryContext)
    fun republishFaktura(fakturanummer: String): UtbetalingLinje = with(tx) {
        queries.utbetalingLinje.getOrError(fakturanummer).also { publishOpprettFaktura(it) }
    }

    context(tx: TransactionalQueryContext)
    fun oppdaterFakturaStatus(
        fakturanummer: String,
        nyStatus: FakturaStatusType,
        fakturaStatusEndretTidspunkt: Instant,
    ): Utbetaling = with(tx) {
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

    context(tx: TransactionalQueryContext)
    fun automatisertUtbetalingVedEttRelevantTilsagn(
        utbetalingId: UUID,
    ): AutomatisertUtbetalingResult = with(tx) {
        val utbetaling = queries.utbetaling.getAndAquireLock(utbetalingId)

        val relevanteTilsagn = queries.tilsagn.getAll(
            gjennomforingId = utbetaling.gjennomforing.id,
            statuser = listOf(TilsagnStatus.GODKJENT),
            typer = listOf(TilsagnType.TILSAGN, TilsagnType.EKSTRATILSAGN),
            periodeIntersectsWith = utbetaling.periode,
        )
        if (relevanteTilsagn.size != 1) {
            return AutomatisertUtbetalingResult.FEIL_ANTALL_TILSAGN
        }

        val tilsagn = queries.tilsagn.getAndAquireLock(relevanteTilsagn[0].id)
        if (tilsagn.gjenstaendeBelop() < utbetaling.beregning.output.pris) {
            return AutomatisertUtbetalingResult.IKKE_NOK_PENGER
        }

        val utbetalingLinjer = queries.utbetalingLinje.getByUtbetalingId(utbetalingId)
        if (utbetalingLinjer.isNotEmpty()) {
            return AutomatisertUtbetalingResult.UTBETALINGLINJER_ALLEREDE_OPPRETTET
        }

        val linje = upsertUtbetalingLinje(
            id = UUID.randomUUID(),
            utbetaling = utbetaling,
            tilsagn = tilsagn,
            pris = utbetaling.beregning.output.pris,
            gjorOppTilsagn = tilsagn.periode.getLastInclusiveDate() in utbetaling.periode,
            behandletAv = Tiltaksadministrasjon,
        )
        return godkjennUtbetalingLinje(linje, Tiltaksadministrasjon)
            .map { AutomatisertUtbetalingResult.GODKJENT }
            .getOrElse { throw UtbetalingException(it) }
    }

    private fun QueryContext.validateAccessAndLockUtbetaling(utbetalingLinjeId: UUID, agent: Agent) = validation {
        val linje = queries.utbetalingLinje.getOrError(utbetalingLinjeId)
        val utbetaling = queries.utbetaling.getAndAquireLock(linje.utbetalingId)
        validate(utbetaling.status == UtbetalingStatusType.TIL_ATTESTERING && linje.status == UtbetalingLinjeStatus.TIL_ATTESTERING) {
            FieldError.of("Utbetaling er ikke satt til attestering")
        }

        when (agent) {
            Arena,
            Arrangor,
            -> error { FieldError.of("$agent kan ikke utføre handlinger på utbetalinger") }

            is NavIdent -> {
                val kostnadssted = queries.tilsagn.getOrError(linje.tilsagnId).kostnadssted
                val ansatt = queries.ansatt.getByNavIdentOrError(agent)
                validate(ansatt.hasKontorspesifikkRolle(Rolle.ATTESTANT_UTBETALING, setOf(kostnadssted.enhetsnummer))) {
                    FieldError.of("Kan ikke attestere utbetalingen fordi du ikke er attestant ved tilsagnets kostnadssted (${kostnadssted.navn})")
                }
            }

            Tiltaksadministrasjon -> Unit
        }

        Pair(utbetaling, linje)
    }

    private suspend fun TransactionalQueryContext.upsert(upsert: UpsertUtbetaling): Either<NonEmptyList<FieldError>, UtbetalingDbo> = when (upsert) {
        is UpsertUtbetaling.Generering -> upsertGenerering(upsert)
        is UpsertUtbetaling.Innsending -> upsertInnsending(upsert)
        is UpsertUtbetaling.Anskaffelse -> upsertAnskaffelse(upsert)
        is UpsertUtbetaling.Korreksjon -> upsertKorreksjon(upsert)
    }

    private suspend fun TransactionalQueryContext.upsertGenerering(
        upsert: UpsertUtbetaling.Generering,
    ): Either<NonEmptyList<FieldError>, UtbetalingDbo> {
        val gjennomforing = queries.gjennomforing.getGjennomforingTiltaksadministrasjon(upsert.gjennomforingId)

        val dbo = UtbetalingDbo(
            id = upsert.id,
            gjennomforingId = upsert.gjennomforingId,
            status = UtbetalingStatusType.GENERERT,
            valuta = upsert.beregning.output.pris.valuta,
            beregning = upsert.beregning,
            periode = upsert.periode,
            tilskuddstype = upsert.tilskuddstype,
            kommentar = null,
            korreksjonGjelderUtbetalingId = null,
            korreksjonBegrunnelse = null,
            journalpostId = null,
            innsendtAvArrangorTidspunkt = null,
            betalingsinformasjon = getUtbetalingsinformasjon(gjennomforing.arrangor.id, upsert.kid),
            utbetalesTidligstTidspunkt = getUtbetalesTidligstTidspunkt(gjennomforing, upsert.periode),
        )

        queries.utbetaling.upsert(dbo)
        queries.utbetaling.setBlokkeringer(dbo.id, upsert.blokkeringer)

        return dbo.right()
    }

    private suspend fun TransactionalQueryContext.upsertInnsending(
        upsert: UpsertUtbetaling.Innsending,
    ): Either<NonEmptyList<FieldError>, UtbetalingDbo> {
        val gjennomforing = queries.gjennomforing.getGjennomforingTiltaksadministrasjon(upsert.gjennomforingId)

        val dbo = UtbetalingDbo(
            id = upsert.id,
            gjennomforingId = upsert.gjennomforingId,
            status = UtbetalingStatusType.TIL_BEHANDLING,
            valuta = upsert.beregning.output.pris.valuta,
            beregning = upsert.beregning,
            periode = upsert.periode,
            tilskuddstype = upsert.tilskuddstype,
            kommentar = null,
            korreksjonGjelderUtbetalingId = null,
            korreksjonBegrunnelse = null,
            journalpostId = null,
            innsendtAvArrangorTidspunkt = LocalDateTime.now(),
            betalingsinformasjon = getUtbetalingsinformasjon(gjennomforing.arrangor.id, upsert.kid),
            utbetalesTidligstTidspunkt = getUtbetalesTidligstTidspunkt(gjennomforing, upsert.periode),
        )

        queries.utbetaling.upsert(dbo)

        return dbo.right()
    }

    private suspend fun TransactionalQueryContext.upsertAnskaffelse(
        upsert: UpsertUtbetaling.Anskaffelse,
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
            tilskuddstype = upsert.tilskuddstype,
            korreksjonGjelderUtbetalingId = null,
            korreksjonBegrunnelse = null,
            journalpostId = upsert.journalpostId,
            innsendtAvArrangorTidspunkt = null,
            betalingsinformasjon = getUtbetalingsinformasjon(gjennomforing.arrangor.id, upsert.kid),
            utbetalesTidligstTidspunkt = getUtbetalesTidligstTidspunkt(gjennomforing, upsert.periode),
        )

        queries.utbetaling.upsert(dbo)

        return dbo.right()
    }

    private suspend fun TransactionalQueryContext.upsertKorreksjon(
        upsert: UpsertUtbetaling.Korreksjon,
    ): Either<NonEmptyList<FieldError>, UtbetalingDbo> {
        val utbetaling = queries.utbetaling.getAndAquireLock(upsert.korreksjonGjelderUtbetalingId)
        if (!utbetaling.erFerdigBehandlet()) {
            return FieldError.of("Utbetaling kan ikke korrigeres når den har status ${utbetaling.status}")
                .nel()
                .left()
        }

        val gjennomforing = queries.gjennomforing.getGjennomforingTiltaksadministrasjon(utbetaling.gjennomforing.id)

        val dbo = UtbetalingDbo(
            id = upsert.id,
            gjennomforingId = gjennomforing.id,
            status = UtbetalingStatusType.TIL_BEHANDLING,
            valuta = upsert.beregning.output.pris.valuta,
            beregning = upsert.beregning,
            periode = upsert.periode,
            tilskuddstype = upsert.tilskuddstype,
            kommentar = upsert.kommentar,
            korreksjonGjelderUtbetalingId = upsert.korreksjonGjelderUtbetalingId,
            korreksjonBegrunnelse = upsert.korreksjonBegrunnelse,
            journalpostId = null,
            innsendtAvArrangorTidspunkt = null,
            betalingsinformasjon = getUtbetalingsinformasjon(gjennomforing.arrangor.id, upsert.kid),
            utbetalesTidligstTidspunkt = getUtbetalesTidligstTidspunkt(gjennomforing, upsert.periode),
        )

        queries.utbetaling.upsert(dbo)

        return dbo.right()
    }

    private suspend fun getUtbetalingsinformasjon(arrangorId: UUID, kid: Kid?): Betalingsinformasjon? {
        return arrangorService.getBetalingsinformasjon(arrangorId)?.let { betalingsinformasjon ->
            when (betalingsinformasjon) {
                is Betalingsinformasjon.BBan -> Betalingsinformasjon.BBan(betalingsinformasjon.kontonummer, kid)
                is Betalingsinformasjon.IBan -> betalingsinformasjon
            }
        }
    }

    private fun getUtbetalesTidligstTidspunkt(
        gjennomforing: GjennomforingTiltaksadministrasjon,
        periode: Periode,
    ): Instant? {
        return config.tidligstTidspunktForUtbetaling.calculate(gjennomforing.tiltakstype.tiltakskode, periode)
    }

    private fun TransactionalQueryContext.logUtbetalingLinjeUtbetalt(
        utbetalingLinje: UtbetalingLinje,
        fakturaStatusEndretTidspunkt: Instant,
    ): Utbetaling {
        val tilsagn = queries.tilsagn.getOrError(utbetalingLinje.tilsagnId)
        return logEndring(
            "Betaling for tilsagn ${tilsagn.bestilling.bestillingsnummer} er utbetalt",
            utbetalingLinje.utbetalingId,
            Tiltaksadministrasjon,
            timestamp = fakturaStatusEndretTidspunkt.tilNorskLocalDateTime(),
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

    private fun TransactionalQueryContext.upsertUtbetalingLinje(
        id: UUID,
        utbetaling: Utbetaling,
        tilsagn: Tilsagn,
        pris: ValutaBelop,
        gjorOppTilsagn: Boolean,
        behandletAv: Agent,
    ): UtbetalingLinje {
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

        totrinnskontroll.opprett(id, TotrinnskontrollType.UTBETALING_LINJE_OPPRETTELSE, behandletAv)

        return queries.utbetalingLinje.getOrError(id)
    }

    private fun TransactionalQueryContext.godkjennUtbetalingLinje(
        utbetalingLinje: UtbetalingLinje,
        besluttetAv: Agent,
    ): Either<List<FieldError>, Utbetaling> {
        val opprettelse = getTotrinnskontroll(utbetalingLinje.id)
        totrinnskontroll.godkjent(opprettelse, besluttetAv).onLeft { return it.left() }
        queries.utbetalingLinje.setStatus(utbetalingLinje.id, UtbetalingLinjeStatus.GODKJENT)

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
            getOrError(utbetalingLinje.utbetalingId)
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
        val opprettelse = getTotrinnskontroll(utbetalingLinjeId)
        val tilsagnTilOppgjor = setTilOppgjor(
            tilsagn,
            opprettelse.behandletAv,
            aarsaker = listOf(),
            forklaring = null,
            operation = "Sendt til oppgjør ved behandling av utbetaling",
        )
        gjorOppTilsagn(
            tilsagnTilOppgjor,
            requireNotNull(opprettelse.besluttetAv),
            operation = "Tilsagn oppgjort ved attestering av utbetaling",
        ).onLeft { errors ->
            throw UtbetalingException(errors)
        }
    }

    private fun TransactionalQueryContext.returnerUtbetalingLinje(
        linje: UtbetalingLinje,
        aarsaker: List<UtbetalingLinjeReturnertAarsak>,
        forklaring: String?,
        besluttetAv: Agent,
    ): Utbetaling {
        setReturnertUtbetalingLinje(linje.id, aarsaker, forklaring, besluttetAv)

        // Sett også de resterende utbetalingslinjene som returnert
        queries.utbetalingLinje.getByUtbetalingId(linje.utbetalingId)
            .filter { it.id != linje.id }
            .forEach {
                // TODO: propagert retur burde kanskje heller opprette nye rader i totrinnskontrollen, heller enn å overskrive besluttelsen?
                setReturnertUtbetalingLinje(
                    it.id,
                    listOf(UtbetalingLinjeReturnertAarsak.PROPAGERT_RETUR),
                    null,
                    Tiltaksadministrasjon,
                )
            }

        queries.utbetaling.setStatus(linje.utbetalingId, UtbetalingStatusType.RETURNERT)
        return logEndring("Utbetaling returnert", linje.utbetalingId, besluttetAv)
    }

    private fun TransactionalQueryContext.setReturnertUtbetalingLinje(
        utbetalingLinjeId: UUID,
        aarsaker: List<UtbetalingLinjeReturnertAarsak>,
        forklaring: String?,
        besluttetAv: Agent,
    ) {
        queries.utbetalingLinje.setStatus(utbetalingLinjeId, UtbetalingLinjeStatus.RETURNERT)
        val opprettelse = getTotrinnskontroll(utbetalingLinjeId)
        totrinnskontroll.avvist(opprettelse, besluttetAv, aarsaker.map { it.name }, forklaring).onLeft {
            throw UtbetalingException(it)
        }
    }

    context(tx: TransactionalQueryContext)
    fun upsertTilsagn(upsert: UpsertTilsagn, agent: Agent): Tilsagn = with(tx) {
        val previous = queries.tilsagn.get(upsert.id)

        val lopenummer = previous?.lopenummer
            ?: queries.tilsagn.getNextLopenummeByGjennomforing(upsert.gjennomforingId)

        val gjennomforingLopenummer = queries.gjennomforing.getGjennomforingTiltaksadministrasjon(upsert.gjennomforingId).lopenummer

        val bestillingsnummer = previous?.bestilling?.bestillingsnummer
            ?: "A-${gjennomforingLopenummer.value}-$lopenummer"

        val dbo = TilsagnDbo(
            id = upsert.id,
            gjennomforingId = upsert.gjennomforingId,
            type = upsert.type,
            periode = upsert.periode,
            lopenummer = lopenummer,
            kostnadssted = upsert.kostnadssted,
            bestillingsnummer = bestillingsnummer,
            bestillingStatus = null,
            belopBrukt = 0.withValuta(upsert.beregning.output.pris.valuta),
            beregning = upsert.beregning,
            kommentar = upsert.kommentar,
            beskrivelse = upsert.beskrivelse,
            deltakere = upsert.deltakere?.map { TilsagnDbo.Deltaker(it.deltakerId, it.innholdAnnet) },
        )

        queries.tilsagn.upsert(dbo)
        totrinnskontroll.opprett(upsert.id, TotrinnskontrollType.TILSAGN_OPPRETTELSE, agent)
        val tilsagn = logEndringTilsagn("Sendt til godkjenning", dbo.id, agent)
        updateFreeTextSearch(dbo)
        tilsagn
    }

    context(tx: TransactionalQueryContext)
    fun godkjennTilsagn(id: UUID, agent: Agent): Either<List<FieldError>, Tilsagn> = with(tx) {
        val tilsagn = queries.tilsagn.getAndAquireLock(id)
        when (tilsagn.status) {
            TilsagnStatus.OPPGJORT, TilsagnStatus.ANNULLERT, TilsagnStatus.GODKJENT, TilsagnStatus.RETURNERT ->
                FieldError.of("Tilsagnet kan ikke godkjennes fordi det har status ${tilsagn.status.beskrivelse}").nel().left()

            TilsagnStatus.TIL_GODKJENNING -> godkjennTilsagnInner(tilsagn, agent).onRight { publishOpprettBestilling(it) }
            TilsagnStatus.TIL_ANNULLERING -> annullerTilsagn(tilsagn, agent).onRight { publishAnnullerBestilling(it) }
            TilsagnStatus.TIL_OPPGJOR -> gjorOppTilsagn(tilsagn, agent, "Tilsagn oppgjort").onRight { publishGjorOppBestilling(it) }
        }
    }

    context(tx: TransactionalQueryContext)
    fun returnerTilsagn(
        tilsagn: Tilsagn,
        besluttetAv: NavIdent,
        aarsaker: List<TilsagnStatusAarsak>,
        forklaring: String?,
    ): Either<List<FieldError>, Tilsagn> = with(tx) {
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
            logEndringTilsagn("Tilsagn returnert", tilsagn.id, besluttetAv)
        }
    }

    context(tx: TransactionalQueryContext)
    fun setTilAnnullering(
        tilsagn: Tilsagn,
        behandletAv: Agent,
        aarsaker: List<String>,
        forklaring: String?,
    ): Tilsagn = with(tx) {
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

        return logEndringTilsagn("Sendt til annullering", tilsagn.id, behandletAv)
    }

    context(tx: TransactionalQueryContext)
    fun annullerTilsagn(
        tilsagn: Tilsagn,
        besluttetAv: Agent,
    ): Either<List<FieldError>, Tilsagn> = with(tx) {
        if (tilsagn.status != TilsagnStatus.TIL_ANNULLERING) {
            return FieldError.of("Tilsagnet må ha status ${TilsagnStatus.TIL_ANNULLERING} for at annullering skal godkjennes")
                .nel()
                .left()
        }
        if (queries.utbetalingLinje.getNextLopenummerByTilsagn(tilsagn.id) > 1) {
            return FieldError.of("Tilsagnet kan ikke annulleres fordi det har blitt brukt i utbetalinger")
                .nel()
                .left()
        }

        val annullering = totrinnskontroll.getOrError(tilsagn.id, TotrinnskontrollType.TILSAGN_ANNULLERING)
        return totrinnskontroll.godkjent(annullering, besluttetAv).map {
            queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.ANNULLERT)
            logEndringTilsagn("Tilsagn annullert", tilsagn.id, besluttetAv)
        }
    }

    context(tx: TransactionalQueryContext)
    fun avvisAnnullering(
        tilsagn: Tilsagn,
        besluttetAv: NavIdent,
        aarsaker: List<TilsagnStatusAarsak>,
        forklaring: String?,
    ): Either<List<FieldError>, Tilsagn> = with(tx) {
        if (tilsagn.status != TilsagnStatus.TIL_ANNULLERING) {
            return FieldError.of("Tilsagnet må ha status ${TilsagnStatus.TIL_ANNULLERING} for at annullering skal avvises")
                .nel()
                .left()
        }

        val annullering = totrinnskontroll.getOrError(tilsagn.id, TotrinnskontrollType.TILSAGN_ANNULLERING)
        return totrinnskontroll.avvist(annullering, besluttetAv, aarsaker.map { it.name }, forklaring).map {
            queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.GODKJENT)
            logEndringTilsagn("Annullering avvist", tilsagn.id, besluttetAv)
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

        return logEndringTilsagn(operation, tilsagn.id, agent)
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
            logEndringTilsagn(operation, tilsagn.id, besluttetAv)
        }
    }

    context(tx: TransactionalQueryContext)
    fun avvisOppgjor(
        tilsagn: Tilsagn,
        besluttetAv: NavIdent,
        aarsaker: List<TilsagnStatusAarsak>,
        forklaring: String?,
    ): Either<List<FieldError>, Tilsagn> = with(tx) {
        if (tilsagn.status != TilsagnStatus.TIL_OPPGJOR) {
            return FieldError.of("Tilsagnet må ha status ${TilsagnStatus.TIL_OPPGJOR} for at oppgjør skal avvises")
                .nel()
                .left()
        }

        val oppgjor = totrinnskontroll.getOrError(tilsagn.id, TotrinnskontrollType.TILSAGN_OPPGJOR)
        return totrinnskontroll.avvist(oppgjor, besluttetAv, aarsaker.map { it.name }, forklaring).map {
            queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.GODKJENT)
            logEndringTilsagn("Oppgjør avvist", tilsagn.id, besluttetAv)
        }
    }

    context(tx: TransactionalQueryContext)
    fun republishOpprettBestilling(bestillingsnummer: String): Tilsagn = with(tx) {
        val tilsagn = queries.tilsagn.getOrError(bestillingsnummer)
        publishOpprettBestilling(tilsagn)
        tilsagn
    }

    private fun TransactionalQueryContext.godkjennTilsagnInner(
        tilsagn: Tilsagn,
        besluttetAv: Agent,
    ): Either<List<FieldError>, Tilsagn> {
        if (tilsagn.status != TilsagnStatus.TIL_GODKJENNING) {
            return FieldError.of("Tilsagnet m ha status ${TilsagnStatus.TIL_GODKJENNING} for å godkjennes")
                .nel()
                .left()
        }

        val opprettelse = totrinnskontroll.getOrError(tilsagn.id, TotrinnskontrollType.TILSAGN_OPPRETTELSE)
        return totrinnskontroll.godkjent(opprettelse, besluttetAv).map {
            queries.tilsagn.setStatus(tilsagn.id, TilsagnStatus.GODKJENT)
            logEndringTilsagn("Tilsagn godkjent", tilsagn.id, besluttetAv)
        }
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

        storeOkonomiMelding(bestilling.bestillingsnummer, OkonomiBestillingMelding.Bestilling(bestilling), null)
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
            null,
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
            null,
        )
    }

    private fun QueryContext.logEndringTilsagn(
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

    private fun TransactionalQueryContext.logEndring(
        operation: String,
        utbetalingId: UUID,
        endretAv: Agent,
        timestamp: LocalDateTime = LocalDateTime.now(),
    ): Utbetaling {
        val utbetaling = getOrError(utbetalingId)
        queries.endringshistorikk.logEndring(
            EndringshistorikkType.UTBETALING,
            operation,
            endretAv,
            utbetalingId,
            timestamp,
        ) {
            Json.encodeToJsonElement(utbetaling)
        }
        return utbetaling
    }

    private fun TransactionalQueryContext.publishOpprettFaktura(linje: UtbetalingLinje) {
        val opprettelse = getTotrinnskontroll(linje.id)
        check(opprettelse.besluttetAv != null && opprettelse.besluttetTidspunkt != null && opprettelse.besluttelse == TotrinnskontrollBesluttelse.GODKJENT) {
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
            is Betalingsinformasjon.BBan -> OpprettFaktura.Betalingsinformasjon.BBan(
                kontonummer = utbetaling.betalingsinformasjon.kontonummer,
                kid = utbetaling.betalingsinformasjon.kid,
            )

            is Betalingsinformasjon.IBan -> OpprettFaktura.Betalingsinformasjon.IBan(
                iban = utbetaling.betalingsinformasjon.iban,
                bic = utbetaling.betalingsinformasjon.bic,
                bankLandKode = utbetaling.betalingsinformasjon.bankLandKode,
                bankNavn = utbetaling.betalingsinformasjon.bankNavn,
            )

            null -> throw IllegalStateException(
                "Betalingsinformasjon mangler for utbetaling med id=${linje.utbetalingId}",
            )
        }

        queries.utbetalingLinje.setFakturaSendtTidspunk(linje.id, Instant.now())

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

    private fun QueryContext.getTotrinnskontroll(utbetalingLinjeId: UUID): Totrinnskontroll {
        return totrinnskontroll.getOrError(utbetalingLinjeId, TotrinnskontrollType.UTBETALING_LINJE_OPPRETTELSE)
    }

    private fun TransactionalQueryContext.storeOkonomiMelding(
        bestillingsnummer: String,
        message: OkonomiBestillingMelding,
        tidspunktForUtbetaling: Instant?,
    ) {
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

    context(tx: TransactionalQueryContext)
    fun getAdvarsler(utbetaling: Utbetaling): List<DeltakerAdvarsel> = with(tx) {
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

    private fun QueryContext.updateFreeTextSearch(tilsagn: TilsagnDbo) {
        val fts = listOf(tilsagn.bestillingsnummer) +
            tilsagn.bestillingsnummer.replace("/", " ") +
            tilsagn.periode.toFreeTextSearch() +
            tilsagn.type.displayName()

        queries.tilsagn.setFreeTextSearch(tilsagn.id, fts)
    }
}
