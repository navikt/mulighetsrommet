package no.nav.mulighetsrommet.database.queries

import kotliquery.Session
import kotliquery.queryOf
import org.intellij.lang.annotations.Language
import java.time.Instant

data class KafkaConsumerRecordDbo(
    val id: Long,
    val topic: String,
    val partition: Int,
    val recordOffset: Long,
    val retries: Int,
    val lastRetry: Instant?,
    val key: ByteArray?,
    val value: ByteArray?,
    val headersJson: String?,
    val recordTimestamp: Long?,
    val createdAt: Instant,
)

class KafkaConsumerRecordQueries(private val session: Session) {
    fun getFailedRecords(): List<KafkaConsumerRecordDbo> {
        @Language("PostgreSQL")
        val query = """
                    select * from kafka_consumer_record
                    where retries > 0
                    order by id
        """.trimIndent()

        return session.list(queryOf(query, emptyMap())) { row ->
            KafkaConsumerRecordDbo(
                id = row.long("id"),
                topic = row.string("topic"),
                partition = row.int("partition"),
                recordOffset = row.long("record_offset"),
                retries = row.int("retries"),
                lastRetry = row.instantOrNull("last_retry"),
                key = row.bytesOrNull("key"),
                value = row.bytesOrNull("value"),
                headersJson = row.stringOrNull("headers_json"),
                recordTimestamp = row.long("record_timestamp"),
                createdAt = row.instant("created_at"),
            )
        }
    }

    fun retryAt(id: Long, topic: String, executionTime: Instant) {
        @Language("PostgreSQL")
        val query = """
                    update kafka_consumer_record
                      set last_retry = :last_retry
                    where id = :id and topic = :topic
        """.trimIndent()

        val params = mapOf(
            "last_retry" to executionTime,
            "id" to id,
            "topic" to topic,
        )
        session.execute(queryOf(query, params))
    }
}
