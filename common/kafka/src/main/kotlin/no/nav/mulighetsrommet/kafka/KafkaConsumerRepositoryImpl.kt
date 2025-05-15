package no.nav.mulighetsrommet.kafka

import kotliquery.Row
import kotliquery.queryOf
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRepository
import no.nav.common.kafka.consumer.feilhandtering.StoredConsumerRecord
import no.nav.mulighetsrommet.database.Database
import org.apache.kafka.common.TopicPartition
import org.intellij.lang.annotations.Language

class KafkaConsumerRepositoryImpl(private val db: Database) : KafkaConsumerRepository {
    override fun storeRecord(record: StoredConsumerRecord): Long {
        @Language("PostgreSQL")
        val query = """
            insert into kafka_consumer_record (topic, partition, record_offset, key, value, headers_json, record_timestamp)
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
            record.timestamp,
        ).asUpdate
        return db.run(queryResult).toLong()
    }

    override fun deleteRecords(ids: MutableList<Long>) {
        @Language("PostgreSQL")
        val query = """
            delete from kafka_consumer_record where id = any(?)
        """.trimIndent()

        val idArray = db.createArrayOf("int8", ids)

        val queryResult = queryOf(query, idArray).asExecute

        db.run(queryResult)
    }

    override fun hasRecordWithKey(topic: String, partition: Int, key: ByteArray): Boolean {
        @Language("PostgreSQL")
        val query = """
            select id from kafka_consumer_record where topic = ? and partition = ? and key = ? limit 1
        """.trimIndent()

        val queryResult = queryOf(query, topic, partition, key).map { it.int("id") }.asSingle

        return db.run(queryResult) != null
    }

    override fun getRecords(topic: String, partition: Int, maxRecords: Int): MutableList<StoredConsumerRecord> {
        @Language("PostgreSQL")
        val query = """
            select * from kafka_consumer_record where topic = ? and partition = ? order by record_offset limit ?
        """.trimIndent()

        val queryResult = queryOf(query, topic, partition, maxRecords).map { toStoredConsumerRecord(it) }.asList

        return db.run(queryResult).toMutableList()
    }

    fun getAll(): MutableList<StoredConsumerRecord> {
        @Language("PostgreSQL")
        val query = """
            select * from kafka_consumer_record order by record_offset
        """.trimIndent()

        val queryResult = queryOf(query).map { toStoredConsumerRecord(it) }.asList

        return db.run(queryResult).toMutableList()
    }

    override fun incrementRetries(id: Long) {
        @Language("PostgreSQL")
        val query = """
            update kafka_consumer_record set retries = retries + 1, last_retry = current_timestamp where id = ?
        """.trimIndent()

        val queryResult = queryOf(query, id).asUpdate

        db.run(queryResult)
    }

    override fun getTopicPartitions(topics: MutableList<String>): MutableList<TopicPartition> {
        @Language("PostgreSQL")
        val query = """
            select distinct topic, partition from kafka_consumer_record where topic = any(?)
        """.trimIndent()

        val topicsArray = db.createArrayOf("varchar", topics)

        val queryResult = queryOf(query, topicsArray)
            .map { TopicPartition(it.string("topic"), it.int("partition")) }
            .asList

        return db.run(queryResult).toMutableList()
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
            row.long("record_timestamp"),
        )
    }
}
