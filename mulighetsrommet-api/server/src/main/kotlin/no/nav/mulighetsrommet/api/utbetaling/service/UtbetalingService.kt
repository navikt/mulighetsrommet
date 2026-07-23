package no.nav.mulighetsrommet.api.utbetaling.service

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.nel
import arrow.core.right
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.admin.arrangor.BetalingsinformasjonQuery
import no.nav.mulighetsrommet.admin.arrangor.HentBetalingsinformasjon
import no.nav.mulighetsrommet.admin.endringshistorikk.EndringshistorikkType
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.TransactionalQueryContext
import no.nav.mulighetsrommet.api.domain.arrangor.Betalingsinformasjon
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsatt
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhet
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.Totrinnskontroll
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollStatus
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingTiltaksadministrasjon
import no.nav.mulighetsrommet.api.tilsagn.TilsagnService
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.api.totrinnskontroll.api.toFieldErrors
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingLinjeDbo
import no.nav.mulighetsrommet.api.utbetaling.model.AutomatisertUtbetalingResult
import no.nav.mulighetsrommet.api.utbetaling.model.OpprettUtbetalingLinje
import no.nav.mulighetsrommet.api.utbetaling.model.UpsertUtbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregning
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFastSatsPerAvtaltTiltaksplassPerManed
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingException
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingLinje
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingLinjeReturnertAarsak
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingLinjeStatus
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.api.utils.DatoUtils.tilNorskLocalDateTime
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.Arena
import no.nav.mulighetsrommet.model.Arrangor
import no.nav.mulighetsrommet.model.FieldError
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltaksadministrasjon
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.validation.Validated
import no.nav.tiltak.okonomi.FakturaStatusType
import no.nav.tiltak.okonomi.OkonomiBestillingMelding
import no.nav.tiltak.okonomi.OpprettFaktura
import no.nav.tiltak.okonomi.toOkonomiPart
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

class UtbetalingService(
    private val config: Config,
    private val tilsagnService: TilsagnService,
    private val betalingsinformasjon: BetalingsinformasjonQuery,
) {
    data class Config(
        val tidligstTidspunktForUtbetaling: TidligstTidspunktForUtbetalingCalculator,
    )

    context(tx: TransactionalQueryContext)
    fun godkjentAvArrangor(
        utbetalingId: UUID,
        kid: Kid?,
    ): Either<List<FieldError>, Unit> = with(tx) {
        val utbetaling = queries.utbetaling.getAndAquireLock(utbetalingId)
        if (utbetaling.status != UtbetalingStatusType.GENERERT) {
            return FieldError.of("Utbetaling er allerede godkjent").nel().left()
        }

        queries.utbetaling.setInnsendtAvArrangor(utbetalingId, LocalDateTime.now())
        queries.utbetaling.setKid(utbetalingId, kid)
        queries.utbetaling.setStatus(utbetalingId, UtbetalingStatusType.TIL_BEHANDLING)

        logEndring("Utbetaling sendt inn", utbetalingId, Arrangor)

        Unit.right()
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

        if (!utbetaling.erTilBehandling() && utbetaling.status != UtbetalingStatusType.GENERERT) {
            return FieldError.of("Utbetalingen kan ikke sendes til attestering").nel().left()
        }

        when (agent) {
            Tiltaksadministrasjon -> Unit

            Arena,
            Arrangor,
            -> return FieldError.of("$agent kan ikke sende utbetaling til attestering").nel().left()

            is NavIdent -> {
                val ansatt = queries.ansatt.getOrError(agent)
                if (!erSaksbehandler(ansatt)) {
                    return FieldError.of("Du kan ikke sende utbetaling til attestering").nel().left()
                }
            }
        }

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
    fun attesterUtbetalingLinje(
        id: UUID,
        agent: Agent,
    ): Either<List<FieldError>, Utbetaling> = with(tx) {
        val linje = queries.utbetalingLinje.getOrError(id)
        val utbetaling = queries.utbetaling.getAndAquireLock(linje.utbetalingId)

        if (utbetaling.status != UtbetalingStatusType.TIL_ATTESTERING || linje.status != UtbetalingLinjeStatus.TIL_ATTESTERING) {
            return FieldError.of("Utbetalingen kan ikke attesteres").nel().left()
        }

        when (agent) {
            Tiltaksadministrasjon -> Unit

            Arena,
            Arrangor,
            -> return FieldError.of("$agent kan ikke attestere utbetalinger").nel().left()

            is NavIdent -> {
                val kostnadssted = queries.tilsagn.getOrError(linje.tilsagnId).kostnadssted
                val ansatt = queries.ansatt.getOrError(agent)
                if (!erAttestant(ansatt, kostnadssted)) {
                    return FieldError.of("Du kan ikke attestere utbetalingen fordi du ikke er attestant ved tilsagnets kostnadssted (${kostnadssted.navn})")
                        .nel()
                        .left()
                }
            }
        }

        attesterUtbetalingLinje(linje, agent)
    }

    context(tx: TransactionalQueryContext)
    fun returnerUtbetalingLinje(
        id: UUID,
        aarsaker: List<UtbetalingLinjeReturnertAarsak>,
        forklaring: String?,
        agent: Agent,
    ): Either<List<FieldError>, Utbetaling> = with(tx) {
        val linje = queries.utbetalingLinje.getOrError(id)
        val utbetaling = queries.utbetaling.getAndAquireLock(linje.utbetalingId)

        if (utbetaling.status != UtbetalingStatusType.TIL_ATTESTERING || linje.status != UtbetalingLinjeStatus.TIL_ATTESTERING) {
            return FieldError.of("Utbetalingen kan ikke returneres").nel().left()
        }

        when (agent) {
            Tiltaksadministrasjon -> Unit

            Arena,
            Arrangor,
            -> return FieldError.of("$agent kan ikke returnere utbetalinger").nel().left()

            is NavIdent -> {
                val kostnadssted = queries.tilsagn.getOrError(linje.tilsagnId).kostnadssted
                val ansatt = queries.ansatt.getOrError(agent)
                if (!(erSaksbehandler(ansatt) || erAttestant(ansatt, kostnadssted))) {
                    return FieldError.of("Du kan ikke returnere utbetalingen fordi du mangler tilgang").nel().left()
                }
            }
        }

        returnerUtbetalingLinje(linje, aarsaker, forklaring, agent).right()
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
        queries.utbetalingLinje.setStatusForLinjer(utbetalingId, UtbetalingLinjeStatus.AVBRUTT)

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
        return attesterUtbetalingLinje(linje, Tiltaksadministrasjon)
            .map { AutomatisertUtbetalingResult.GODKJENT }
            .getOrElse { throw UtbetalingException(it) }
    }

    context(tx: TransactionalQueryContext)
    fun automatisertUtbetalingFastSatsPerAvtaltTiltaksplassPerManed(
        utbetaling: Utbetaling,
    ): AutomatisertUtbetalingResult = with(tx) {
        val beregning = utbetaling.beregning as? UtbetalingBeregningFastSatsPerAvtaltTiltaksplassPerManed
            ?: return AutomatisertUtbetalingResult.FEIL_PRISMODELL

        val linjer = beregning.output.tilsagnBidrag.map {
            OpprettUtbetalingLinje(
                id = UUID.randomUUID(),
                tilsagnId = it.tilsagnId,
                pris = it.bidrag,
                gjorOppTilsagn = false,
            )
        }
        sendTilAttestering(utbetaling.id, linjer, Tiltaksadministrasjon).onLeft { throw UtbetalingException(it) }

        linjer.forEach { linje ->
            attesterUtbetalingLinje(linje.id, Tiltaksadministrasjon).onLeft { throw UtbetalingException(it) }
        }

        AutomatisertUtbetalingResult.GODKJENT
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
        return betalingsinformasjon.execute(HentBetalingsinformasjon(arrangorId))?.let { betalingsinformasjon ->
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

        val opprettelse = Totrinnskontroll.opprett(
            UUID.randomUUID(),
            id,
            TotrinnskontrollType.UTBETALING_LINJE_OPPRETTELSE,
            behandletAv,
        )
        queries.totrinnskontroll.upsert(opprettelse)
        outbox.publish(opprettelse)

        return queries.utbetalingLinje.getOrError(id)
    }

    private fun TransactionalQueryContext.attesterUtbetalingLinje(
        utbetalingLinje: UtbetalingLinje,
        besluttetAv: Agent,
    ): Either<List<FieldError>, Utbetaling> {
        val opprettelse = getTotrinnskontroll(utbetalingLinje.id)
        opprettelse.godkjenn(besluttetAv).mapLeft { it.toFieldErrors() }.onLeft { return it.left() }.onRight { godkjent ->
            queries.totrinnskontroll.upsert(godkjent)
            outbox.publish(godkjent)
        }
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
        opprettelse.returner(besluttetAv, aarsaker.map { it.name }, forklaring).mapLeft { it.toFieldErrors() }.onLeft {
            throw UtbetalingException(it)
        }.onRight { returnert ->
            queries.totrinnskontroll.upsert(returnert)
            outbox.publish(returnert)
        }
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
        val besluttetAv = opprettelse.besluttetAv
        val besluttetTidspunkt = opprettelse.besluttetTidspunkt
        check(besluttetAv != null && besluttetTidspunkt != null && opprettelse.status == TotrinnskontrollStatus.GODKJENT) {
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
            besluttetAv = besluttetAv.toOkonomiPart(),
            besluttetTidspunkt = besluttetTidspunkt,
            gjorOppBestilling = linje.gjorOppTilsagn,
            beskrivelse = beskrivelse,
            belop = linje.pris.belop,
            valuta = linje.pris.valuta,
        )

        val tidspunktForUtbetaling = linje.faktura.utbetalesTidligstTidspunkt
            ?: config.tidligstTidspunktForUtbetaling.calculate(tilsagn.tiltakstype.tiltakskode, faktura.periode)
        val message = OkonomiBestillingMelding.Faktura(faktura)
        outbox.publish(message, tidspunktForUtbetaling)
    }

    private fun QueryContext.getTotrinnskontroll(utbetalingLinjeId: UUID): Totrinnskontroll {
        return queries.totrinnskontroll.getOrError(utbetalingLinjeId, TotrinnskontrollType.UTBETALING_LINJE_OPPRETTELSE)
    }

    private fun QueryContext.getOrError(id: UUID): Utbetaling {
        return queries.utbetaling.getOrError(id)
    }
}

fun erSaksbehandler(ansatt: NavAnsatt): Boolean = ansatt.hasGenerellRolle(Rolle.SAKSBEHANDLER_OKONOMI)

fun erAttestant(ansatt: NavAnsatt, kostnadssted: NavEnhet): Boolean {
    return ansatt.hasKontorspesifikkRolle(Rolle.ATTESTANT_UTBETALING, setOf(kostnadssted.enhetsnummer))
}

fun erBeslutter(ansatt: NavAnsatt, kostnadssted: NavEnhet): Boolean {
    return ansatt.hasKontorspesifikkRolle(Rolle.BESLUTTER_TILSAGN, setOf(kostnadssted.enhetsnummer))
}
