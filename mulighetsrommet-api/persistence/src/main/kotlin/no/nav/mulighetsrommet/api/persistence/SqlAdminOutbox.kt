package no.nav.mulighetsrommet.api.persistence

import kotlinx.serialization.json.Json
import kotliquery.Session
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord
import no.nav.mulighetsrommet.admin.QueryContext
import no.nav.mulighetsrommet.api.domain.tiltak.Tiltakstype
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.Totrinnskontroll
import no.nav.mulighetsrommet.api.persistence.tiltak.TiltakstypeQueries
import no.nav.mulighetsrommet.api.persistence.totrinnskontroll.toTotrinnskontrollHendelse
import no.nav.mulighetsrommet.kafka.KafkaProducerRecordQueries

data class OutboxTopics(
    val sisteTiltakstyperV3: String,
    val totrinnskontrollHendelseV1: String,
)

class SqlAdminOutbox(session: Session, private val topics: OutboxTopics) : QueryContext.Outbox {
    private val kpr = KafkaProducerRecordQueries(session)
    private val tiltakstype = TiltakstypeQueries(session)

    override fun publish(tiltakstype: Tiltakstype) {
        val dto = requireNotNull(this@SqlAdminOutbox.tiltakstype.getEksternTiltakstype(tiltakstype.id)) {
            "Fant ikke ekstern tiltakstype for id=${tiltakstype.id}"
        }
        val record = StoredProducerRecord(
            topics.sisteTiltakstyperV3,
            dto.id.toString().toByteArray(),
            Json.encodeToString(dto).toByteArray(),
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
