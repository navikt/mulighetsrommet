package no.nav.mulighetsrommet.api.totrinnskontroll

import kotlinx.serialization.json.Json
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.TransactionalQueryContext
import no.nav.mulighetsrommet.api.totrinnskontroll.db.TotrinnskontrollDbo
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Besluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollHendelse
import no.nav.mulighetsrommet.model.Agent
import java.time.LocalDateTime
import java.util.UUID

class TotrinnskontrollService(private val topic: String) {

    context(tx: QueryContext)
    fun get(entityId: UUID, type: Totrinnskontroll.Type): Totrinnskontroll? = with(tx) {
        queries.totrinnskontroll.get(entityId, type)
    }

    context(tx: QueryContext)
    fun getOrError(entityId: UUID, type: Totrinnskontroll.Type): Totrinnskontroll = with(tx) {
        queries.totrinnskontroll.getOrError(entityId, type)
    }

    context(tx: TransactionalQueryContext)
    fun opprett(
        entityId: UUID,
        type: Totrinnskontroll.Type,
        behandletAv: Agent,
        aarsaker: List<String> = emptyList(),
        forklaring: String? = null,
    ) {
        val dbo = TotrinnskontrollDbo(
            id = UUID.randomUUID(),
            entityId = entityId,
            type = type,
            behandletAv = behandletAv,
            behandletTidspunkt = LocalDateTime.now(),
            besluttetAv = null,
            besluttetTidspunkt = null,
            besluttelse = null,
            aarsaker = aarsaker,
            forklaring = forklaring,
        )
        upsert(dbo)
    }

    context(tx: TransactionalQueryContext)
    fun godkjent(
        existing: Totrinnskontroll,
        besluttetAv: Agent,
    ) {
        val dbo = TotrinnskontrollDbo(
            id = existing.id,
            entityId = existing.entityId,
            type = existing.type,
            behandletAv = existing.behandletAv,
            behandletTidspunkt = existing.behandletTidspunkt,
            besluttetAv = besluttetAv,
            besluttetTidspunkt = LocalDateTime.now(),
            besluttelse = Besluttelse.GODKJENT,
            aarsaker = existing.aarsaker,
            forklaring = existing.forklaring,
        )
        upsert(dbo)
    }

    context(tx: TransactionalQueryContext)
    fun avvist(
        existing: Totrinnskontroll,
        besluttetAv: Agent,
        aarsaker: List<String> = emptyList(),
        forklaring: String? = null,
    ) {
        val dbo = TotrinnskontrollDbo(
            id = existing.id,
            entityId = existing.entityId,
            type = existing.type,
            behandletAv = existing.behandletAv,
            behandletTidspunkt = existing.behandletTidspunkt,
            besluttetAv = besluttetAv,
            besluttetTidspunkt = LocalDateTime.now(),
            besluttelse = Besluttelse.AVVIST,
            aarsaker = aarsaker.ifEmpty { existing.aarsaker },
            forklaring = forklaring ?: existing.forklaring,
        )
        upsert(dbo)
    }

    context(tx: TransactionalQueryContext)
    private fun upsert(dbo: TotrinnskontrollDbo) = with(tx) {
        queries.totrinnskontroll.upsert(dbo)
        val hendelse = TotrinnskontrollHendelse(
            id = dbo.id,
            entityId = dbo.entityId,
            type = dbo.type,
            behandletAv = dbo.behandletAv,
            behandletTidspunkt = dbo.behandletTidspunkt,
            besluttetAv = dbo.besluttetAv,
            besluttetTidspunkt = dbo.besluttetTidspunkt,
            besluttelse = dbo.besluttelse,
            aarsaker = dbo.aarsaker,
            forklaring = dbo.forklaring,
        )
        queries.kafkaProducerRecord.storeRecord(
            StoredProducerRecord(
                topic,
                hendelse.entityId.toString().toByteArray(),
                Json.encodeToString(hendelse).toByteArray(),
                null,
            ),
        )
    }
}
