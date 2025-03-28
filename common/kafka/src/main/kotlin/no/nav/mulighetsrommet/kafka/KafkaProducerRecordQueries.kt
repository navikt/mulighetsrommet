package no.nav.mulighetsrommet.kafka

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord
import no.nav.mulighetsrommet.database.createTextArray
import no.nav.mulighetsrommet.database.requireSingle
import org.intellij.lang.annotations.Language

class KafkaProducerRecordQueries(private val session: Session) {
    fun storeRecord(record: StoredProducerRecord): Long {
        @Language("PostgreSQL")
        val sql = """
            insert into kafka_producer_record (topic, key, value, headers_json) values (?, ?, ?, ?)
            returning id
        """.trimIndent()

        val query = queryOf(sql, record.topic, record.key, record.value, record.headersJson)

        return session.requireSingle(query) { it.long("id") }
    }

    fun deleteRecords(ids: List<Long>) {
        @Language("PostgreSQL")
        val sql = """
            delete from kafka_producer_record where id = any(?::bigint[])
        """.trimIndent()

        session.update(queryOf(sql, session.createArrayOf("bigint", ids)))
    }

    fun getRecords(maxMessages: Int): List<StoredProducerRecord> {
        @Language("PostgreSQL")
        val sql = """
            select * from kafka_producer_record order by id limit ?
        """.trimIndent()

        val query = queryOf(sql, maxMessages)

        return session.list(query) { it.toStoredProducerRecord() }
    }

    fun getRecords(maxMessages: Int, topics: List<String>): List<StoredProducerRecord> {
        @Language("PostgreSQL")
        val sql = """
            select * from kafka_producer_record where topic = any(?::text[]) order by id limit ?
        """.trimIndent()

        val query = queryOf(sql, session.createTextArray(topics), maxMessages)

        return session.list(query) { it.toStoredProducerRecord() }
    }
}

fun Row.toStoredProducerRecord() = StoredProducerRecord(
    long("id"),
    string("topic"),
    bytes("key"),
    bytes("value"),
    stringOrNull("headers_json"),
)
