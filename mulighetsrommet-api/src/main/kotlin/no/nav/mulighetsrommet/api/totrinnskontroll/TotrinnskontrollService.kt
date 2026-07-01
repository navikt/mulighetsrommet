package no.nav.mulighetsrommet.api.totrinnskontroll

import arrow.core.Either
import arrow.core.NonEmptyList
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.TransactionalQueryContext
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.totrinnskontroll.db.toDbo
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollType
import no.nav.mulighetsrommet.model.Agent
import java.util.UUID

class TotrinnskontrollService {

    context(tx: QueryContext)
    fun get(entityId: UUID, type: TotrinnskontrollType): Totrinnskontroll? = with(tx) {
        queries.totrinnskontroll.get(entityId, type)
    }

    context(tx: QueryContext)
    fun getOrError(entityId: UUID, type: TotrinnskontrollType): Totrinnskontroll = with(tx) {
        queries.totrinnskontroll.getOrError(entityId, type)
    }

    context(tx: TransactionalQueryContext)
    fun opprett(
        entityId: UUID,
        type: TotrinnskontrollType,
        behandletAv: Agent,
        aarsaker: List<String> = emptyList(),
        forklaring: String? = null,
    ) = opprett(UUID.randomUUID(), entityId, type, behandletAv, aarsaker, forklaring)

    context(tx: TransactionalQueryContext)
    fun opprett(
        id: UUID,
        entityId: UUID,
        type: TotrinnskontrollType,
        behandletAv: Agent,
        aarsaker: List<String> = emptyList(),
        forklaring: String? = null,
    ): Totrinnskontroll {
        val tilBehandling = Totrinnskontroll.opprett(
            id = id,
            entityId = entityId,
            type = type,
            behandletAv = behandletAv,
            aarsaker = aarsaker,
            forklaring = forklaring,
        )
        upsert(tilBehandling)
        return tilBehandling
    }

    context(tx: TransactionalQueryContext)
    fun tilbakestill(
        existing: Totrinnskontroll,
        nyBehandletAv: Agent,
    ): Either<NonEmptyList<FieldError>, Totrinnskontroll> {
        return existing.tilbakestill(nyBehandletAv).onRight { upsert(it) }
    }

    context(tx: TransactionalQueryContext)
    fun godkjent(
        existing: Totrinnskontroll,
        besluttetAv: Agent,
    ): Either<NonEmptyList<FieldError>, Totrinnskontroll> {
        return existing.godkjenn(besluttetAv).onRight { upsert(it) }
    }

    context(tx: TransactionalQueryContext)
    fun returnert(
        existing: Totrinnskontroll,
        besluttetAv: Agent,
        aarsaker: List<String> = emptyList(),
        forklaring: String? = null,
    ): Either<NonEmptyList<FieldError>, Totrinnskontroll> {
        return existing.returner(besluttetAv, aarsaker, forklaring).onRight { upsert(it) }
    }

    context(tx: TransactionalQueryContext)
    fun sattPaVent(
        existing: Totrinnskontroll,
        besluttetAv: Agent,
        aarsaker: List<String> = emptyList(),
        forklaring: String? = null,
    ): Either<NonEmptyList<FieldError>, Totrinnskontroll> {
        return existing.settPaVent(besluttetAv, aarsaker, forklaring).onRight { upsert(it) }
    }

    context(tx: TransactionalQueryContext)
    private fun upsert(totrinnskontroll: Totrinnskontroll) {
        val dbo = totrinnskontroll.toDbo()
        tx.queries.totrinnskontroll.upsert(dbo)
        tx.outbox.publish(totrinnskontroll)
    }
}
