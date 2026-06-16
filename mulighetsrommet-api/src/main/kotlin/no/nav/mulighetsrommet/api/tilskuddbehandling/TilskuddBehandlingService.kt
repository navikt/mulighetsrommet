package no.nav.mulighetsrommet.api.tilskuddbehandling

import arrow.core.Either
import arrow.core.left
import arrow.core.nel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.TransactionalQueryContext
import no.nav.mulighetsrommet.api.endringshistorikk.EndringshistorikkType
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingDetaljerDto
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingDto
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingHandling
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingKompakt
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingRequest
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingStatus
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingStatusAarsak
import no.nav.mulighetsrommet.api.tilskuddbehandling.task.JournalforVedtaksbrev
import no.nav.mulighetsrommet.api.totrinnskontroll.TotrinnskontrollService
import no.nav.mulighetsrommet.api.totrinnskontroll.api.toDto
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollType
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingException
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID
import kotlin.String

class TilskuddBehandlingService(
    private val db: ApiDatabase,
    private val journalforVedtaksbrev: JournalforVedtaksbrev,
    private val totrinnskontroll: TotrinnskontrollService,
) {
    fun upsert(
        request: TilskuddBehandlingRequest,
        navIdent: NavIdent,
    ): Either<List<FieldError>, Unit> {
        val gjennomforing = db.session { queries.gjennomforing.getGjennomforing(request.gjennomforingId) }
            ?: throw IllegalStateException("Fant ikke gjennomføring for tilskuddsbehandling")

        return TilskuddBehandlingValidator
            .validate(request, gjennomforing)
            .map { dbo ->
                db.transaction {
                    queries.tilskuddBehandling.upsert(dbo)
                    totrinnskontroll.opprett(dbo.id, TotrinnskontrollType.TILSKUDD_OPPRETTELSE, navIdent)
                    logEndring("Sendt til attestering", dbo.id, navIdent)
                }
            }
    }

    fun getByGjennomforingId(gjennomforingId: UUID): List<TilskuddBehandlingKompakt> {
        return db.session {
            queries.tilskuddBehandling.getByGjennomforingId(gjennomforingId)
                .map {
                    TilskuddBehandlingKompakt(
                        id = it.id,
                        soknadDato = it.soknadDato,
                        periode = it.periode,
                        journalpostId = it.soknadJournalpostId,
                        tilskuddtyper = it.tilskudd.map { tilskudd -> tilskudd.tilskuddOpplaeringType }
                            .toSet(),
                        kostnadssted = it.kostnadssted,
                        status = it.status,
                    )
                }
        }
    }

    fun getDetaljerDto(id: UUID, navIdent: NavIdent): TilskuddBehandlingDetaljerDto? {
        return db.session {
            val behandling = queries.tilskuddBehandling.get(id)
            behandling?.let {
                TilskuddBehandlingDetaljerDto(
                    it,
                    totrinnskontroll.getOrError(id, TotrinnskontrollType.TILSKUDD_OPPRETTELSE).toDto(),
                    handlinger(it, navIdent),
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

            val opprettelse = totrinnskontroll.getOrError(id, TotrinnskontrollType.TILSKUDD_OPPRETTELSE)
            totrinnskontroll.godkjent(opprettelse, navIdent)
                .map {
                    queries.tilskuddBehandling.setStatus(id, TilskuddBehandlingStatus.FERDIG_BEHANDLET)
                    scheduleJournalforVedtak(id)
            logEndring("Tilskuddsbehandling attestert", behandling.id, navIdent)}
        }
    } catch (e: UtbetalingException) {
        e.errors.left()
    }

    private fun TransactionalQueryContext.scheduleJournalforVedtak(behandlingId: UUID) {
        journalforVedtaksbrev.schedule(
            vedtakId = behandlingId,
            startTime = Instant.now(),
            tx = session,
        )
    }

    fun returner(
        id: UUID,
        navIdent: NavIdent,
        aarsaker: List<TilskuddBehandlingStatusAarsak>,
        forklaring: String?,
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

        val opprettelse = totrinnskontroll.getOrError(id, TotrinnskontrollType.TILSKUDD_OPPRETTELSE)
        totrinnskontroll.avvist(opprettelse, navIdent, aarsaker.map { it.name }, forklaring).map {
            queries.tilskuddBehandling.setStatus(id, TilskuddBehandlingStatus.RETURNERT)
            logEndring("Tilskuddsbehandling returnert", behandling.id, navIdent)
        }
    }

    fun handlinger(behandling: TilskuddBehandlingDto, navIdent: NavIdent): Set<TilskuddBehandlingHandling> = db.session {
        val opprettelse = totrinnskontroll.getOrError(behandling.id, TotrinnskontrollType.TILSKUDD_OPPRETTELSE)

        return setOfNotNull(
            TilskuddBehandlingHandling.REDIGER.takeIf { behandling.status.type == TilskuddBehandlingStatus.RETURNERT },
            TilskuddBehandlingHandling.ATTESTER.takeIf { behandling.status.type == TilskuddBehandlingStatus.TIL_ATTESTERING },
            TilskuddBehandlingHandling.RETURNER.takeIf { behandling.status.type == TilskuddBehandlingStatus.TIL_ATTESTERING },
        )
            .filter {
                tilgangTilHandling(
                    handling = it,
                    navIdent = navIdent,
                    kostnadssted = behandling.kostnadssted.enhetsnummer,
                    opprettelse = opprettelse,
                )
            }
            .toSet()
    }

    fun tilgangTilHandling(
        handling: TilskuddBehandlingHandling,
        navIdent: NavIdent,
        kostnadssted: NavEnhetNummer,
        opprettelse: Totrinnskontroll,
    ): Boolean {
        val ansatt = db.session { queries.ansatt.getByNavIdent(navIdent) }
            ?: throw IllegalStateException("Fant ikke ansatt med navIdent $navIdent")

        val attestant = ansatt.hasKontorspesifikkRolle(Rolle.ATTESTANT_UTBETALING, setOf(kostnadssted))
        val saksbehandler = ansatt.hasGenerellRolle(Rolle.SAKSBEHANDLER_OKONOMI)

        return when (handling) {
            TilskuddBehandlingHandling.REDIGER,
            -> saksbehandler

            TilskuddBehandlingHandling.RETURNER,
            -> saksbehandler || attestant

            TilskuddBehandlingHandling.ATTESTER -> {
                attestant && opprettelse.behandletAv != ansatt.navIdent
            }
        }
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
}
