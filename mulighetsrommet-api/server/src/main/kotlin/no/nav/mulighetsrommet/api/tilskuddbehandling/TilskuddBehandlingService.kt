package no.nav.mulighetsrommet.api.tilskuddbehandling

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.nel
import arrow.core.right
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.admin.endringshistorikk.EndringshistorikkType
import no.nav.mulighetsrommet.admin.totrinnskontroll.TotrinnskontrollDto
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.TransactionalQueryContext
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.Totrinnskontroll
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.api.pdfgen.PdfGenClient
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddBehandling
import no.nav.mulighetsrommet.api.tilskuddbehandling.mapper.TilskuddVedtakToPdfDocumentContentMapper
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingDetaljerDto
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingDto
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingHandling
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingKompakt
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingRequest
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingStatus
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingStatusAarsak
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingStatusDto
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingType
import no.nav.mulighetsrommet.api.tilskuddbehandling.task.JournalforVedtaksbrev
import no.nav.mulighetsrommet.api.tilskuddbehandling.task.hentForhandsvisningVedtaksbrevInnhold
import no.nav.mulighetsrommet.api.totrinnskontroll.api.toFieldErrors
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingException
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.FieldError
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.ProblemDetail
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

class TilskuddBehandlingService(
    private val db: ApiDatabase,
    private val journalforVedtaksbrev: JournalforVedtaksbrev,
    private val pdf: PdfGenClient,
) {
    fun upsert(
        request: TilskuddBehandlingRequest,
        navIdent: NavIdent,
    ): Either<List<FieldError>, Unit> {
        val gjennomforing = db.session { queries.gjennomforing.getGjennomforing(request.gjennomforingId) }
            ?: throw IllegalStateException("Fant ikke gjennomføring for tilskuddsbehandling")
        val behandlendeEnhet = db.session { queries.ansatt.get(navIdent) }?.hovedenhet
            ?: throw IllegalArgumentException("Fant ikke enhet for ansatt $navIdent")

        return TilskuddBehandlingValidator
            .validate(request, gjennomforing, behandlendeEnhet)
            .map { dbo ->
                db.transaction {
                    queries.tilskuddBehandling.upsert(dbo)
                    val opprettelse = when (dbo.type) {
                        TilskuddBehandlingType.REGISTRERING -> Totrinnskontroll.opprett(
                            UUID.randomUUID(),
                            dbo.id,
                            TotrinnskontrollType.TILSKUDD_OPPRETTELSE,
                            navIdent,
                        )

                        TilskuddBehandlingType.REVURDERING -> Totrinnskontroll.opprett(
                            UUID.randomUUID(),
                            dbo.id,
                            TotrinnskontrollType.TILSKUDD_OPPHOR,
                            navIdent,
                        )
                    }
                    queries.totrinnskontroll.upsert(opprettelse)
                    outbox.publish(opprettelse)
                    logEndring("Sendt til attestering", dbo.id, navIdent)
                }
            }
    }

    fun getByGjennomforingId(gjennomforingId: UUID): List<TilskuddBehandlingKompakt> {
        return db.session {
            queries.tilskuddBehandling.getByGjennomforingId(gjennomforingId)
                .map {
                    val førsteTilskudd = it.tilskudd.firstOrNull()
                        ?: error("Tilskuddsbehandling med id=${it.id} mangler tilskudd")
                    TilskuddBehandlingKompakt(
                        id = it.id,
                        soknadDato = førsteTilskudd.soknadDato,
                        periode = førsteTilskudd.periode,
                        journalpostId = førsteTilskudd.soknadJournalpostId,
                        tilskuddtyper = it.tilskudd.map { tilskudd -> tilskudd.tilskuddOpplaeringType }
                            .toSet(),
                        kostnadssted = førsteTilskudd.kostnadssted,
                        status = it.status,
                        type = it.type,
                        samletVedtakResultat = it.samletVedtakResultat,
                    )
                }
        }
    }

    fun getDetaljerDto(id: UUID, navIdent: NavIdent): TilskuddBehandlingDetaljerDto? {
        return db.session {
            val behandling = queries.tilskuddBehandling.get(id)
            behandling?.let {
                val totrinnskontroll = when (behandling.type) {
                    TilskuddBehandlingType.REGISTRERING -> queries.totrinnskontroll.getDtoOrError(
                        behandling.id,
                        TotrinnskontrollType.TILSKUDD_OPPRETTELSE,
                    )

                    TilskuddBehandlingType.REVURDERING -> queries.totrinnskontroll.getDtoOrError(
                        behandling.id,
                        TotrinnskontrollType.TILSKUDD_OPPHOR,
                    )
                }
                TilskuddBehandlingDetaljerDto(
                    it,
                    totrinnskontroll,
                    handlinger(it, navIdent, totrinnskontroll),
                )
            }
        }
    }

    fun attester(
        id: UUID,
        navIdent: NavIdent,
    ): Either<List<FieldError>, TilskuddBehandlingDto> = try {
        db.transaction {
            val behandling = requireNotNull(queries.tilskuddBehandling.get(id)) {
                "TilskuddBehandling med id $id ble ikke funnet"
            }
            if (behandling.status.type !== TilskuddBehandlingStatus.TIL_ATTESTERING) {
                return FieldError
                    .of("Tilskuddsbehandling kan ikke attesteres fordi det har status ${behandling.status.type.beskrivelse}")
                    .nel()
                    .left()
            }
            val kontrollType = when (behandling.type) {
                TilskuddBehandlingType.REGISTRERING -> TotrinnskontrollType.TILSKUDD_OPPRETTELSE
                TilskuddBehandlingType.REVURDERING -> TotrinnskontrollType.TILSKUDD_OPPHOR
            }
            val totrinnskontroll = queries.totrinnskontroll.getOrError(id, kontrollType)

            totrinnskontroll.godkjenn(navIdent)
                .mapLeft { it.toFieldErrors() }
                .map { godkjent ->
                    queries.totrinnskontroll.upsert(godkjent)
                    outbox.publish(godkjent)
                    queries.tilskuddBehandling.setStatus(id, TilskuddBehandlingStatus.FERDIG_BEHANDLET)
                    scheduleJournalforVedtak(id)
                    logEndring("Tilskuddsbehandling attestert", behandling.id, navIdent)
                }
        }
    } catch (e: UtbetalingException) {
        e.errors.left()
    }

    private fun TransactionalQueryContext.scheduleJournalforVedtak(behandlingId: UUID) {
        journalforVedtaksbrev.schedule(
            behandlingId = behandlingId,
            startTime = Instant.now(),
            tx = session,
        )
    }

    fun returner(
        id: UUID,
        navIdent: NavIdent,
        aarsaker: List<TilskuddBehandlingStatusAarsak>,
        begrunnelse: String?,
    ): Either<List<FieldError>, TilskuddBehandlingDto> = db.transaction {
        val behandling = requireNotNull(queries.tilskuddBehandling.get(id)) {
            "TilskuddBehandling med id $id ble ikke funnet"
        }
        if (behandling.status.type !== TilskuddBehandlingStatus.TIL_ATTESTERING) {
            return FieldError
                .of("Tilskuddsbehandling kan ikke returneres fordi det har status ${behandling.status.type.beskrivelse}")
                .nel()
                .left()
        }
        val kontrollType = when (behandling.type) {
            TilskuddBehandlingType.REGISTRERING -> TotrinnskontrollType.TILSKUDD_OPPRETTELSE
            TilskuddBehandlingType.REVURDERING -> TotrinnskontrollType.TILSKUDD_OPPHOR
        }
        queries.totrinnskontroll.getOrError(id, kontrollType)
            .returner(navIdent, begrunnelse, aarsaker.map { it.name })
            .mapLeft { it.toFieldErrors() }
            .map { returnert ->
                queries.totrinnskontroll.upsert(returnert)
                outbox.publish(returnert)
                queries.tilskuddBehandling.setStatus(id, TilskuddBehandlingStatus.RETURNERT)
                logEndring("Tilskuddsbehandling returnert", behandling.id, navIdent)
            }
    }

    fun handlinger(
        behandling: TilskuddBehandlingDto,
        navIdent: NavIdent,
        totrinnskontroll: TotrinnskontrollDto,
    ): Set<TilskuddBehandlingHandling> = db.session {
        return setOfNotNull(
            TilskuddBehandlingHandling.REDIGER.takeIf { behandling.status.type == TilskuddBehandlingStatus.RETURNERT },
            TilskuddBehandlingHandling.ATTESTER.takeIf { behandling.status.type == TilskuddBehandlingStatus.TIL_ATTESTERING },
            TilskuddBehandlingHandling.RETURNER.takeIf { behandling.status.type == TilskuddBehandlingStatus.TIL_ATTESTERING },
        )
            .filter {
                val kostnadssted = behandling.tilskudd.firstOrNull()?.kostnadssted?.enhetsnummer
                    ?: error("Tilskuddsbehandling med id=${behandling.id} mangler tilskudd")
                tilgangTilHandling(
                    handling = it,
                    navIdent = navIdent,
                    kostnadssted = kostnadssted,
                    totrinnskontroll = totrinnskontroll,
                )
            }
            .toSet()
    }

    fun tilgangTilHandling(
        handling: TilskuddBehandlingHandling,
        navIdent: NavIdent,
        kostnadssted: NavEnhetNummer,
        totrinnskontroll: TotrinnskontrollDto,
    ): Boolean {
        val ansatt = db.session { queries.ansatt.getOrError(navIdent) }

        val attestant = ansatt.hasKontorspesifikkRolle(Rolle.ATTESTANT_UTBETALING, setOf(kostnadssted))
        val saksbehandler = ansatt.hasGenerellRolle(Rolle.SAKSBEHANDLER_OKONOMI)
        val erIkkeBehandletAvAnsatt = totrinnskontroll.behandling.utfortAv.agent != ansatt.navIdent

        return when (handling) {
            TilskuddBehandlingHandling.REDIGER,
            -> saksbehandler

            TilskuddBehandlingHandling.RETURNER,
            -> saksbehandler || attestant

            TilskuddBehandlingHandling.ATTESTER -> {
                attestant && erIkkeBehandletAvAnsatt
            }
        }
    }

    fun revurderingOpphor(
        forrigeTilskuddVedtakId: UUID,
        behandlingId: UUID,
        saksbehandler: NavIdent,
    ): Either<List<FieldError>, UUID> = db.transaction {
        val tidligereBehandling = queries.tilskuddBehandling.get(behandlingId)
            ?: throw IllegalStateException("Fant ikke tilskuddsbehandling for behandlingId=$behandlingId")
        val forrigeTilskuddVedtak = tidligereBehandling.tilskudd.firstOrNull { it.id == forrigeTilskuddVedtakId }
            ?: throw IllegalStateException("Fant ikke tilskudd for tilskuddVedtakId=$forrigeTilskuddVedtakId i behandlingId=$behandlingId")

        queries.tilskuddBehandling.acquireLockTilskudd(forrigeTilskuddVedtak.tilskuddId)
        val behandlendeEnhet = db.session { queries.ansatt.get(saksbehandler) }?.hovedenhet
            ?: throw IllegalArgumentException("Fant ikke enhet for ansatt $saksbehandler")

        val opphorRevurdering = tidligereBehandling.copy(
            id = UUID.randomUUID(),
            type = TilskuddBehandlingType.REVURDERING,
            status = TilskuddBehandlingStatusDto(TilskuddBehandlingStatus.TIL_ATTESTERING),
            tilskudd = listOf(
                forrigeTilskuddVedtak.copy(
                    id = UUID.randomUUID(),
                    utbetalingBelop = forrigeTilskuddVedtak.utbetalingBelop?.copy(belop = 0),
                ),
            ),
            behandlendeEnhet = behandlendeEnhet,
        ).toDbo()

        queries.tilskuddBehandling.upsert(opphorRevurdering)
        revurderingOpphorTotrinnkontroll(
            opphorRevurdering.id,
            listOf(TilskuddBehandlingStatusAarsak.ANNET),
            "Test av opphør",
            saksbehandler,
        )

        opphorRevurdering.id.right()
    }

    context(tx: TransactionalQueryContext)
    private fun revurderingOpphorTotrinnkontroll(
        behandlingId: UUID,
        aarsaker: List<TilskuddBehandlingStatusAarsak>,
        begrunnelse: String?,
        behandletAv: Agent,
    ): Totrinnskontroll = with(tx) {
        val opphorTotrinnskontroll = Totrinnskontroll.opprett(
            id = UUID.randomUUID(),
            entityId = behandlingId,
            type = TotrinnskontrollType.TILSKUDD_OPPHOR,
            behandletAv = behandletAv,
            behandletBegrunnelse = begrunnelse,
            behandletAarsaker = aarsaker.map { it.name },
        )
        queries.totrinnskontroll.upsert(opphorTotrinnskontroll)
        outbox.publish(opphorTotrinnskontroll)
        return opphorTotrinnskontroll
    }

    private fun QueryContext.logEndring(
        operation: String,
        id: UUID,
        endretAv: Agent,
    ): TilskuddBehandlingDto {
        val behandling = queries.tilskuddBehandling.getOrError(id)
        queries.endringshistorikk.logEndring(
            EndringshistorikkType.TILSKUDD_BEHANDLING,
            operation,
            endretAv,
            id,
            LocalDateTime.now(),
        ) {
            Json.encodeToJsonElement(behandling)
        }
        return behandling
    }

    suspend fun vedtaksbrevForhandsvisPdf(
        request: TilskuddBehandlingRequest,
        navIdent: NavIdent,
    ): Either<List<FieldError>, ByteArray> = db.session {
        val gjennomforing = db.session { queries.gjennomforing.getGjennomforing(request.gjennomforingId) }
            ?: throw IllegalStateException("Fant ikke gjennomføring for tilskuddsbehandling")
        val behandlendeEnhet = db.session { queries.ansatt.get(navIdent) }?.hovedenhet
            ?: throw IllegalArgumentException("Fant ikke enhet for ansatt $navIdent")

        return TilskuddBehandlingValidator
            .validate(request, gjennomforing, behandlendeEnhet)
            .map { dbo ->
                vedtaksbrevForhandsvisPdf(dbo).getOrElse { throw IllegalStateException("Klarte ikke lage vedtaksbrev pdf") }
            }
    }

    suspend fun vedtaksbrevForhandsvisPdf(id: UUID): Either<ProblemDetail, ByteArray> = db.session {
        return vedtaksbrevForhandsvisPdf(queries.tilskuddBehandling.getOrError(id).toDbo())
    }

    private suspend fun vedtaksbrevForhandsvisPdf(tilskuddBehandling: TilskuddBehandling): Either<ProblemDetail, ByteArray> = db.transaction {
        val gjennomforing =
            queries.gjennomforing.getGjennomforingEnkeltplassOrError(tilskuddBehandling.gjennomforingId)

        val innhold = hentForhandsvisningVedtaksbrevInnhold(
            tilskuddBehandling = tilskuddBehandling,
            gjennomforing = gjennomforing,
        ).fold(
            { error -> throw IllegalStateException("Klarte ikke hente innhold for vedtaksbrev: $error") },
            { it },
        )

        val mappedContent = TilskuddVedtakToPdfDocumentContentMapper.toPdfDocumentContent(
            innhold,
        )

        return pdf.getPdfDocument(mappedContent)
    }
}
