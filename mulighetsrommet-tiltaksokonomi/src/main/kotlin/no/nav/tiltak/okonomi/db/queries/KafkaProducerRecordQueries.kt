package no.nav.tiltak.okonomi.db.queries

import kotlinx.serialization.json.Json
import kotliquery.Session
import kotliquery.queryOf
import no.nav.common.kafka.producer.util.ProducerUtils
import no.nav.tiltak.okonomi.api.BestillingStatus
import no.nav.tiltak.okonomi.api.FakturaStatus
import org.apache.kafka.clients.producer.ProducerRecord
import org.intellij.lang.annotations.Language

class KafkaProducerRecordQueries(private val session: Session) {

    fun insertBestillingStatus(topic: String, status: BestillingStatus) {
        val record = ProducerRecord(
            topic,
            status.bestillingsnummer.toByteArray(),
            Json.encodeToString(status).toByteArray(),
        )

        insert(record)
    }

    fun insertFakturaStatus(topic: String, status: FakturaStatus) {
        val record = ProducerRecord(
            topic,
            status.fakturanummer.toByteArray(),
            Json.encodeToString(status).toByteArray(),
        )

        insert(record)
    }

    private fun insert(record: ProducerRecord<ByteArray?, ByteArray?>) {
        val storedRecord = ProducerUtils.mapToStoredRecord(record)

        @Language("PostgreSQL")
        val sql = """
            insert into kafka_producer_record (topic, key, value, headers_json) values (?, ?, ?, ?)
        """.trimIndent()

        val query = queryOf(sql, storedRecord.topic, storedRecord.key, storedRecord.value, storedRecord.headersJson)

        session.execute(query)
    }
}
