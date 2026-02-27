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
    val recordTimestamp: Instant?,
    val createdAt: Instant,
)

class KafkaConsumerRecordQueries(private val session: Session) {
    fun getFailedRecords(): List<KafkaConsumerRecordDbo> {
        @Language("PostgreSQL")
        val query = """
                    select * from kafka_consumer_record
                    where retries > 0
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
                recordTimestamp = row.instantOrNull("record_timestamp"),
                createdAt = row.instant("created_at"),
            )
        }
    }
}
