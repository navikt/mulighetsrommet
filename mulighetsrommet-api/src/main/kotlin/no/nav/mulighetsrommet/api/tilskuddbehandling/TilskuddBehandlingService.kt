package no.nav.mulighetsrommet.api.tilskuddbehandling

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingDto
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingRequest
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
        val opprettelse = queries.totrinnskontroll.getOrError(id, Totrinnskontroll.Type.OPPRETT)

        if (navIdent == opprettelse.behandletAv) {
            return@transaction listOf(FieldError.of("Du kan ikke beslutte en behandling du selv har opprettet")).left()
        }

        val godkjentOpprettelse = opprettelse.copy(
            besluttetAv = navIdent,
            besluttetTidspunkt = LocalDateTime.now(),
            besluttelse = Besluttelse.GODKJENT,
        )
        queries.totrinnskontroll.upsert(godkjentOpprettelse.toDbo())

        behandling.right()
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
        val opprettelse = queries.totrinnskontroll.getOrError(id, Totrinnskontroll.Type.OPPRETT)

        val avvistOpprettelse = opprettelse.copy(
            besluttetAv = navIdent,
            besluttetTidspunkt = LocalDateTime.now(),
            besluttelse = Besluttelse.AVVIST,
            aarsaker = aarsaker,
            forklaring = forklaring,
        )
        queries.totrinnskontroll.upsert(avvistOpprettelse.toDbo())

        behandling.right()
    }

    fun getByGjennomforingId(gjennomforingId: UUID): List<TilskuddBehandlingDto> {
        return db.session {
            queries.tilskuddBehandling.getByGjennomforingId(gjennomforingId)
        }
    }
}
