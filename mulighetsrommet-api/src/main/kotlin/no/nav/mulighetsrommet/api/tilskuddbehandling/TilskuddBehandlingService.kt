package no.nav.mulighetsrommet.api.tilskuddbehandling

import arrow.core.Either
import arrow.core.left
import arrow.core.nel
import arrow.core.right
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingDto
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingRequest
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingStatus
import no.nav.mulighetsrommet.api.totrinnskontroll.db.TotrinnskontrollDbo
import no.nav.mulighetsrommet.api.totrinnskontroll.db.toDbo
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Besluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.model.NavIdent
import java.time.LocalDateTime
import java.util.UUID

class TilskuddBehandlingService(private val db: ApiDatabase) {
    fun opprett(
        request: TilskuddBehandlingRequest,
        navIdent: NavIdent,
    ): Either<List<FieldError>, TilskuddBehandlingDto> {
        return TilskuddBehandlingValidator
            .validate(request)
            .map { dbo ->
                db.transaction {
                    queries.tilskuddBehandling.insert(dbo)

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

                    requireNotNull(queries.tilskuddBehandling.get(dbo.id)) {
                        "TilskuddBehandling med id ${dbo.id} ble ikke funnet etter insert"
                    }
                }
            }
    }

    fun get(id: UUID): TilskuddBehandlingDto? {
        return db.session {
            queries.tilskuddBehandling.get(id)
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
                .of("Tilskuddsbehandling kan ikke attesteres fordi den har status ${behandling.status.type.beskrivelse}")
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

        queries.tilskuddBehandling.getOrError(id).right()
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
                .of("Tilskuddsbehandling kan ikke returneres fordi den har status ${behandling.status.type.beskrivelse}")
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

        queries.tilskuddBehandling.getOrError(id).right()
    }

    fun getByGjennomforingId(gjennomforingId: UUID): List<TilskuddBehandlingDto> {
        return db.session {
            queries.tilskuddBehandling.getByGjennomforingId(gjennomforingId)
        }
    }
}
