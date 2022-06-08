package no.nav.mulighetsrommet.arena.adapter

import kotliquery.Row
import kotliquery.queryOf
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRepository
import no.nav.common.kafka.consumer.feilhandtering.StoredConsumerRecord
import org.apache.kafka.common.TopicPartition
import org.intellij.lang.annotations.Language

class KafkaConsumerRepository(private val db: Database) : KafkaConsumerRepository {
    override fun storeRecord(record: StoredConsumerRecord): Long {
        @Language("PostgreSQL")
        val query = """
            insert into failed_events (topic, partition, record_offset, key, value, headers_json, record_timestamp)
            values (?, ?, ?, ?, ?, ?, ?)
            on conflict (topic, partition, record_offset) do nothing
        """.trimIndent()
        val queryResult = queryOf(
            query,
            record.topic,
            record.partition,
            record.offset,
            record.key,
            record.value,
            record.headersJson,
            record.timestamp
        ).asUpdate
        return db.session.run(queryResult).toLong()
    }

    override fun deleteRecords(ids: MutableList<Long>) {
        @Language("PostgreSQL")
        val query = """
            delete from failed_events where id = any(?)
        """.trimIndent()

        val idArray = db.session.createArrayOf("int8", ids)

        val queryResult = queryOf(query, idArray).asExecute

        db.session.run(queryResult)
    }

    override fun hasRecordWithKey(topic: String, partition: Int, key: ByteArray): Boolean {
        @Language("PostgreSQL")
        val query = """
            select id from failed_events where topic = ? and partition = ? and key = ? limit 1
        """.trimIndent()

        val queryResult = queryOf(query, topic, partition, key).map { it.int("id") }.asSingle

        return db.session.run(queryResult) != null
    }

    override fun getRecords(topic: String, partition: Int, maxRecords: Int): MutableList<StoredConsumerRecord> {
        @Language("PostgreSQL")
        val query = """
            select * from failed_events where topic = ? and partition = ? order by record_offset limit ?
        """.trimIndent()

        val queryResult = queryOf(query, topic, partition, maxRecords).map { toStoredConsumerRecord(it) }.asList

        return db.session.run(queryResult).toMutableList()
    }

    override fun incrementRetries(id: Long) {
        @Language("PostgreSQL")
        val query = """
            update failed_events set retries = retries + 1, last_retry = current_timestamp where id = ?
        """.trimIndent()

        val queryResult = queryOf(query, id).asUpdate

        db.session.run(queryResult)
    }

    override fun getTopicPartitions(topics: MutableList<String>): MutableList<TopicPartition> {
        @Language("PostgreSQL")
        val query = """
            select distinct topic, partition from failed_events where topic = any(?)
        """.trimIndent()

        val topicsArray = db.session.createArrayOf("varchar", topics)

        val queryResult = queryOf(query, topicsArray)
            .map { TopicPartition(it.string("topic"), it.int("partition")) }
            .asList

        return db.session.run(queryResult).toMutableList()
    }

    private fun toStoredConsumerRecord(row: Row): StoredConsumerRecord {
        return StoredConsumerRecord(
            row.long("id"),
            row.string("topic"),
            row.int("partition"),
            row.long("record_offset"),
            row.bytesOrNull("key"),
            row.bytesOrNull("value"),
            row.stringOrNull("headers_json"),
            row.int("retries"),
            row.sqlTimestampOrNull("last_retry"),
            row.long("record_timestamp")
        )
    }
}
