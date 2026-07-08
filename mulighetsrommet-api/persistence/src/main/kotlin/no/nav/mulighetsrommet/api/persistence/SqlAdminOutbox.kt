package no.nav.mulighetsrommet.api.persistence

import kotlinx.serialization.json.Json
import kotliquery.Session
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord
import no.nav.mulighetsrommet.admin.QueryContext
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.Totrinnskontroll
import no.nav.mulighetsrommet.api.persistence.totrinnskontroll.toTotrinnskontrollHendelse
import no.nav.mulighetsrommet.kafka.KafkaProducerRecordQueries
import no.nav.mulighetsrommet.model.TiltakstypeV3Dto

data class OutboxTopics(
    val sisteTiltakstyperV3: String,
    val totrinnskontrollHendelseV1: String,
)

class SqlAdminOutbox(session: Session, private val topics: OutboxTopics) : QueryContext.Outbox {
    val kpr = KafkaProducerRecordQueries(session)

    override fun publish(ekstern: TiltakstypeV3Dto) {
        val record = StoredProducerRecord(
            topics.sisteTiltakstyperV3,
            ekstern.id.toString().toByteArray(),
            Json.encodeToString(ekstern).toByteArray(),
            null,
        )
        kpr.storeRecord(record)
    }

    override fun publish(totrinnskontroll: Totrinnskontroll) {
        val hendelse = totrinnskontroll.toTotrinnskontrollHendelse()
        val record = StoredProducerRecord(
            topics.totrinnskontrollHendelseV1,
            totrinnskontroll.entityId.toString().toByteArray(),
            Json.encodeToString(hendelse).toByteArray(),
            null,
        )
        kpr.storeRecord(record)
    }
}
