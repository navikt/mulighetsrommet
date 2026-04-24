package no.nav.mulighetsrommet.api.tilskuddbehandling

import arrow.core.Either
import arrow.core.left
import arrow.core.nel
import arrow.core.right
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandling
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingDto
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingHandling
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingKompakt
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingRequest
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingStatus
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingStatusDto
import no.nav.mulighetsrommet.api.totrinnskontroll.api.toDto
import no.nav.mulighetsrommet.api.totrinnskontroll.db.TotrinnskontrollDbo
import no.nav.mulighetsrommet.api.totrinnskontroll.db.toDbo
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Besluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
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
                }
            }
    }

    fun get(id: UUID, navIdent: NavIdent): TilskuddBehandlingDto? {
        return db.session {
            val behandling = queries.tilskuddBehandling.get(id)
            behandling?.let {
                TilskuddBehandlingDto.from(
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
    ): Either<List<FieldError>, Unit> = db.transaction {
        val behandling = requireNotNull(queries.tilskuddBehandling.get(id)) {
            "TilskuddBehandling med id $id ble ikke funnet"
        }
        if (behandling.status !== TilskuddBehandlingStatus.TIL_GODKJENNING) {
            return FieldError
                .of("Tilskuddsbehandling kan ikke godkjennes fordi det har status ${behandling.status.beskrivelse}")
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
        queries.tilskuddBehandling.setStatus(id, TilskuddBehandlingStatus.GODKJENT)
        Unit.right()
    }

    fun returner(
        id: UUID,
        navIdent: NavIdent,
        aarsaker: List<String>,
        forklaring: String?,
    ): Either<List<FieldError>, Unit> = db.transaction {
        val behandling = requireNotNull(queries.tilskuddBehandling.get(id)) {
            "TilskuddBehandling med id $id ble ikke funnet"
        }
        if (behandling.status !== TilskuddBehandlingStatus.TIL_GODKJENNING) {
            return FieldError
                .of("Tilskuddsbehandling kan ikke returneres fordi det har status ${behandling.status.beskrivelse}")
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

        Unit.right()
    }

    fun getByGjennomforingId(gjennomforingId: UUID): List<TilskuddBehandlingKompakt> {
        return db.session {
            queries.tilskuddBehandling.getByGjennomforingId(gjennomforingId)
                .map {
                    TilskuddBehandlingKompakt(
                        id = it.id,
                        soknadDato = it.soknadDato,
                        periode = it.periode,
                        kostnadssted = it.kostnadssted,
                        status = TilskuddBehandlingStatusDto(it.status),
                    )
                }
        }
    }

    fun handlinger(behandling: TilskuddBehandling, navIdent: NavIdent): Set<TilskuddBehandlingHandling> = db.session {
        val opprettelse = queries.totrinnskontroll.getOrError(behandling.id, Totrinnskontroll.Type.OPPRETT)

        return setOfNotNull(
            TilskuddBehandlingHandling.REDIGER.takeIf { behandling.status == TilskuddBehandlingStatus.RETURNERT },
            TilskuddBehandlingHandling.ATTESTER.takeIf { behandling.status == TilskuddBehandlingStatus.TIL_GODKJENNING },
            TilskuddBehandlingHandling.RETURNER.takeIf { behandling.status == TilskuddBehandlingStatus.TIL_GODKJENNING },
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
}
