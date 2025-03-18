package no.nav.mulighetsrommet.kafka

import kotliquery.Row
import kotliquery.queryOf
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRepository
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.createTextArray
import org.intellij.lang.annotations.Language

class KafkaProducerRepositoryImpl(private val db: Database) : KafkaProducerRepository {
    override fun storeRecord(record: StoredProducerRecord): Long = db.session { session ->
        @Language("PostgreSQL")
        val sql = """
            insert into kafka_producer_record (topic, key, value, headers_json) values (?, ?, ?, ?)
            returning id
        """.trimIndent()

        val query = queryOf(sql, record.topic, record.key, record.value, record.headersJson)

        session.single(query) { it.long("id") }!!
    }

    override fun deleteRecords(ids: List<Long>): Unit = db.session { session ->
        @Language("PostgreSQL")
        val sql = """
            delete from kafka_producer_record where id = any(?::bigint[])
        """.trimIndent()

        session.update(queryOf(sql, session.createArrayOf("bigint", ids)))
    }

    override fun getRecords(maxMessages: Int): List<StoredProducerRecord> = db.session { session ->
        @Language("PostgreSQL")
        val sql = """
            select * from kafka_producer_record order by id limit ?
        """.trimIndent()

        val query = queryOf(sql, maxMessages)

        session.list(query) { toStoredProducerRecord(it) }
    }

    override fun getRecords(maxMessages: Int, topics: List<String>): List<StoredProducerRecord> = db.session { session ->
        @Language("PostgreSQL")
        val sql = """
            select * from kafka_producer_record where topic = any(?::text[]) order by id limit ?
        """.trimIndent()

        val query = queryOf(sql, session.createTextArray(topics), maxMessages)

        session.list(query) { toStoredProducerRecord(it) }
    }
}

private fun toStoredProducerRecord(row: Row) = StoredProducerRecord(
    row.long("id"),
    row.string("topic"),
    row.bytes("key"),
    row.bytes("value"),
    row.string("headers_json"),
)
