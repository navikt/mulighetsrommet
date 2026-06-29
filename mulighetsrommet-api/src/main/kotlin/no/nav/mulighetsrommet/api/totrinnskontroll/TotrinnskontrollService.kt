package no.nav.mulighetsrommet.api.totrinnskontroll

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.left
import arrow.core.nel
import arrow.core.right
import kotlinx.serialization.json.Json
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.TransactionalQueryContext
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.totrinnskontroll.db.TotrinnskontrollDbo
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollHendelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollStatus
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollType
import no.nav.mulighetsrommet.api.totrinnskontroll.model.toAgentHendelse
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.NavIdent
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class TotrinnskontrollService(private val topic: String) {

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
    ) {
        val dbo = TotrinnskontrollDbo(
            id = id,
            entityId = entityId,
            type = type,
            status = TotrinnskontrollStatus.TIL_BEHANDLING,
            behandletAv = behandletAv,
            behandletTidspunkt = instantAsMicros(),
            besluttetAv = null,
            besluttetTidspunkt = null,
            aarsaker = aarsaker,
            forklaring = forklaring,
        )
        upsert(dbo)
    }

    context(tx: TransactionalQueryContext)
    fun tilbakestill(
        existing: Totrinnskontroll,
        nyBehandletAv: Agent,
    ): Either<NonEmptyList<FieldError>, TotrinnskontrollDbo> {
        if (existing.status != TotrinnskontrollStatus.SATT_PA_VENT) {
            return FieldError.of("Totrinnskontrollen kan bare tilbakestilles når den er satt på vent").nel().left()
        }
        val dbo = TotrinnskontrollDbo(
            id = existing.id,
            entityId = existing.entityId,
            type = existing.type,
            status = TotrinnskontrollStatus.TIL_BEHANDLING,
            behandletAv = nyBehandletAv,
            behandletTidspunkt = instantAsMicros(),
            besluttetAv = null,
            besluttetTidspunkt = null,
            aarsaker = existing.aarsaker,
            forklaring = null,
        )
        upsert(dbo)
        return dbo.right()
    }

    context(tx: TransactionalQueryContext)
    fun godkjent(
        existing: Totrinnskontroll,
        besluttetAv: Agent,
    ): Either<NonEmptyList<FieldError>, TotrinnskontrollDbo> {
        if (existing.status == TotrinnskontrollStatus.GODKJENT) {
            return alleredeBesluttetError(existing.status)
        }
        if (besluttetAv is NavIdent && besluttetAv == existing.behandletAv) {
            return FieldError.of("Du kan ikke beslutte noe du selv har behandlet").nel().left()
        }
        val dbo = TotrinnskontrollDbo(
            id = existing.id,
            entityId = existing.entityId,
            type = existing.type,
            behandletAv = existing.behandletAv,
            behandletTidspunkt = existing.behandletTidspunkt,
            besluttetAv = besluttetAv,
            besluttetTidspunkt = instantAsMicros(),
            status = TotrinnskontrollStatus.GODKJENT,
            aarsaker = existing.aarsaker,
            forklaring = existing.forklaring,
        )
        upsert(dbo)
        return dbo.right()
    }

    context(tx: TransactionalQueryContext)
    fun returnert(
        existing: Totrinnskontroll,
        besluttetAv: Agent,
        aarsaker: List<String> = emptyList(),
        forklaring: String? = null,
    ): Either<NonEmptyList<FieldError>, TotrinnskontrollDbo> {
        if (existing.status != TotrinnskontrollStatus.TIL_BEHANDLING && besluttetAv is NavIdent) {
            return alleredeBesluttetError(existing.status)
        }
        val dbo = TotrinnskontrollDbo(
            id = existing.id,
            entityId = existing.entityId,
            type = existing.type,
            behandletAv = existing.behandletAv,
            behandletTidspunkt = existing.behandletTidspunkt,
            besluttetAv = besluttetAv,
            besluttetTidspunkt = instantAsMicros(),
            status = TotrinnskontrollStatus.RETURNERT,
            aarsaker = aarsaker.ifEmpty { existing.aarsaker },
            forklaring = forklaring ?: existing.forklaring,
        )
        upsert(dbo)
        return dbo.right()
    }

    context(tx: TransactionalQueryContext)
    fun sattPaVent(
        existing: Totrinnskontroll,
        besluttetAv: Agent,
        aarsaker: List<String> = emptyList(),
        forklaring: String? = null,
    ): Either<NonEmptyList<FieldError>, TotrinnskontrollDbo> {
        if (existing.status != TotrinnskontrollStatus.TIL_BEHANDLING && besluttetAv is NavIdent) {
            return alleredeBesluttetError(existing.status)
        }
        val dbo = TotrinnskontrollDbo(
            id = existing.id,
            entityId = existing.entityId,
            type = existing.type,
            behandletAv = existing.behandletAv,
            behandletTidspunkt = existing.behandletTidspunkt,
            besluttetAv = besluttetAv,
            besluttetTidspunkt = instantAsMicros(),
            status = TotrinnskontrollStatus.SATT_PA_VENT,
            aarsaker = aarsaker.ifEmpty { existing.aarsaker },
            forklaring = forklaring ?: existing.forklaring,
        )
        upsert(dbo)
        return dbo.right()
    }

    context(tx: TransactionalQueryContext)
    private fun upsert(dbo: TotrinnskontrollDbo) {
        tx.queries.totrinnskontroll.upsert(dbo)
        val hendelse = TotrinnskontrollHendelse(
            id = dbo.id,
            entityId = dbo.entityId,
            type = dbo.type,
            behandletAv = dbo.behandletAv.toAgentHendelse(),
            behandletTidspunkt = dbo.behandletTidspunkt,
            besluttetAv = dbo.besluttetAv?.toAgentHendelse(),
            besluttetTidspunkt = dbo.besluttetTidspunkt,
            // TODO: introdusere ny tilstand på topic når komet er klar for mottakelse
            besluttelse = when (dbo.status) {
                TotrinnskontrollStatus.TIL_BEHANDLING -> null
                TotrinnskontrollStatus.GODKJENT -> TotrinnskontrollHendelse.Besluttelse.GODKJENT
                TotrinnskontrollStatus.RETURNERT -> TotrinnskontrollHendelse.Besluttelse.AVVIST
                TotrinnskontrollStatus.SATT_PA_VENT -> TotrinnskontrollHendelse.Besluttelse.AVVIST
            },
            aarsaker = dbo.aarsaker,
            forklaring = dbo.forklaring,
        )
        tx.queries.kafkaProducerRecord.storeRecord(
            StoredProducerRecord(
                topic,
                hendelse.entityId.toString().toByteArray(),
                Json.encodeToString(hendelse).toByteArray(),
                null,
            ),
        )
    }
}

private fun instantAsMicros(): Instant = Instant.now().truncatedTo(ChronoUnit.MICROS)

private fun alleredeBesluttetError(status: TotrinnskontrollStatus): Either<NonEmptyList<FieldError>, Nothing> {
    val besluttelse = when (status) {
        TotrinnskontrollStatus.RETURNERT -> "returnert"
        TotrinnskontrollStatus.GODKJENT -> "godkjent"
        TotrinnskontrollStatus.SATT_PA_VENT -> "satt på vent"
        TotrinnskontrollStatus.TIL_BEHANDLING -> error("Totrinnskontroll er til behandling")
    }
    return FieldError.of("Totrinnskontrollen er allerede $besluttelse").nel().left()
}
