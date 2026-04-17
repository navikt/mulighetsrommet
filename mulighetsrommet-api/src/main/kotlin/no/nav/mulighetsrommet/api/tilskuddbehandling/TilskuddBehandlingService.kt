package no.nav.mulighetsrommet.api.tilskuddbehandling

import arrow.core.Either
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingDto
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingRequest
import no.nav.mulighetsrommet.api.totrinnskontroll.db.TotrinnskontrollDbo
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

    fun getByGjennomforingId(gjennomforingId: UUID): List<TilskuddBehandlingDto> {
        return db.session {
            queries.tilskuddBehandling.getByGjennomforingId(gjennomforingId)
        }
    }
}
