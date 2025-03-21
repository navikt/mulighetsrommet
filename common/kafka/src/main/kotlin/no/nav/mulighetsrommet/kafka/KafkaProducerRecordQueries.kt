package no.nav.mulighetsrommet.kafka

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.common.kafka.util.KafkaUtils
import org.apache.kafka.clients.producer.ProducerRecord
import org.intellij.lang.annotations.Language

class KafkaProducerRecordQueries(private val session: Session) {
    fun insert(record: ProducerRecord<ByteArray, ByteArray?>) {
        @Language("PostgreSQL")
        val sql = """
            insert into kafka_producer_record (topic, key, value, headers_json) values (?, ?, ?, ?)
        """.trimIndent()

        val query = queryOf(sql, record.topic(), record.key(), record.value(), KafkaUtils.headersToJson(record.headers()))

        session.execute(query)
    }

    fun deleteRecords(ids: List<Long>) {
        @Language("PostgreSQL")
        val sql = """
            delete from kafka_producer_record where id = any(?::bigint[])
        """.trimIndent()

        session.update(queryOf(sql, session.createArrayOf("bigint", ids)))
    }

    fun getRecords(): List<ProducerRecordDbo> {
        @Language("PostgreSQL")
        val sql = """
            select distinct on (topic) * from kafka_producer_record order by topic, id
        """.trimIndent()

        val query = queryOf(sql)

        return session.list(query) { it.toProducerRecordDbo() }
    }
}

fun Row.toProducerRecordDbo() = ProducerRecordDbo(
    long("id"),
    string("topic"),
    bytes("key"),
    bytes("value"),
    stringOrNull("headers_json"),
)

data class ProducerRecordDbo(
    val id: Long? = 0,
    val topic: String,
    val key: ByteArray,
    val value: ByteArray?,
    val headersJson: String?,
)
