package no.nav.mulighetsrommet.api.tilskuddbehandling

import arrow.core.Either
import arrow.core.left
import arrow.core.nel
import arrow.core.right
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingDetaljerDto
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingDto
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingHandling
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingKompakt
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingRequest
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingStatus
import no.nav.mulighetsrommet.api.totrinnskontroll.api.toDto
import no.nav.mulighetsrommet.api.totrinnskontroll.db.TotrinnskontrollDbo
import no.nav.mulighetsrommet.api.totrinnskontroll.db.toDbo
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Besluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import java.time.LocalDateTime
import java.util.UUID

class TilskuddBehandlingService(private val db: ApiDatabase) {
    fun upsert(
        request: TilskuddBehandlingRequest,
        navIdent: NavIdent,
    ): Either<List<FieldError>, Unit> {
        return TilskuddBehandlingValidator
            .validate(request)
            .map { dbo ->
                db.transaction {
                    queries.tilskuddBehandling.upsert(dbo)

                    queries.totrinnskontroll.upsert(
                        TotrinnskontrollDbo(
                            id = UUID.randomUUID(),
                            entityId = dbo.id,
                            type = Totrinnskontroll.Type.OPPRETT,
                            behandletAv = navIdent,
                            behandletTidspunkt = LocalDateTime.now(),
                            besluttelse = null,
                            besluttetAv = null,
                            besluttetTidspunkt = null,
                            aarsaker = emptyList(),
                            forklaring = null,
                        ),
                    )

                    logEndring("Sendt til attestering", dbo.id, navIdent)
                }
            }
    }

    fun getDetaljerDto(id: UUID, navIdent: NavIdent): TilskuddBehandlingDetaljerDto? {
        return db.session {
            val behandling = queries.tilskuddBehandling.get(id)
            behandling?.let {
                TilskuddBehandlingDetaljerDto(
                    it,
                    requireNotNull(queries.totrinnskontroll.get(id, Totrinnskontroll.Type.OPPRETT)).toDto(),
                    handlinger(it, navIdent),
                )
            }
        }
    }

    fun godkjenn(
        id: UUID,
        navIdent: NavIdent,
    ): Either<List<FieldError>, TilskuddBehandlingDto> = db.transaction {
        val behandling = requireNotNull(queries.tilskuddBehandling.get(id)) {
            "TilskuddBehandling med id $id ble ikke funnet"
        }
        if (behandling.status.type !== TilskuddBehandlingStatus.TIL_ATTESTERING) {
            return FieldError
                .of("Tilskuddsbehandling kan ikke godkjennes fordi det har status ${behandling.status.type.beskrivelse}")
                .nel()
                .left()
        }

        val opprettelse = queries.totrinnskontroll.getOrError(id, Totrinnskontroll.Type.OPPRETT)
        if (navIdent == opprettelse.behandletAv) {
            return FieldError
                .of("Du kan ikke beslutte en tilskuddsbehandling du selv har opprettet")
                .nel()
                .left()
        }

        val godkjentOpprettelse = opprettelse.copy(
            besluttetAv = navIdent,
            besluttetTidspunkt = LocalDateTime.now(),
            besluttelse = Besluttelse.GODKJENT,
        )
        queries.totrinnskontroll.upsert(godkjentOpprettelse.toDbo())
        queries.tilskuddBehandling.setStatus(id, TilskuddBehandlingStatus.FERDIG_BEHANDLET)

        logEndring("Tilskuddsbehandling attestert", behandling.id, navIdent).right()
    }

    fun returner(
        id: UUID,
        navIdent: NavIdent,
        aarsaker: List<String>,
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

        val opprettelse = queries.totrinnskontroll.getOrError(id, Totrinnskontroll.Type.OPPRETT)

        val avvistOpprettelse = opprettelse.copy(
            besluttetAv = navIdent,
            besluttetTidspunkt = LocalDateTime.now(),
            besluttelse = Besluttelse.AVVIST,
            aarsaker = aarsaker,
            forklaring = forklaring,
        )
        queries.totrinnskontroll.upsert(avvistOpprettelse.toDbo())
        queries.tilskuddBehandling.setStatus(id, TilskuddBehandlingStatus.RETURNERT)

        logEndring("Tilskuddsbehandling returnert", behandling.id, navIdent).right()
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

    fun handlinger(behandling: TilskuddBehandlingDto, navIdent: NavIdent): Set<TilskuddBehandlingHandling> = db.session {
        val opprettelse = queries.totrinnskontroll.getOrError(behandling.id, Totrinnskontroll.Type.OPPRETT)

        return setOfNotNull(
            TilskuddBehandlingHandling.REDIGER.takeIf { behandling.status.type == TilskuddBehandlingStatus.RETURNERT },
            TilskuddBehandlingHandling.ATTESTER.takeIf { behandling.status.type == TilskuddBehandlingStatus.TIL_ATTESTERING },
            TilskuddBehandlingHandling.RETURNER.takeIf { behandling.status.type == TilskuddBehandlingStatus.TIL_ATTESTERING },
        )
            .filter {
                tilgangTilHandling(
                    handling = it,
                    navIdent = navIdent,
                    kostnadssted = behandling.kostnadssted,
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
            -> attestant

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
            DocumentClass.TILSKUDD_BEHANDLING,
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
