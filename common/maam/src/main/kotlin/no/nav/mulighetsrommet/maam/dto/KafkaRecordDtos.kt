package no.nav.mulighetsrommet.maam.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import java.time.LocalDateTime

@Serializable
data class RetryKafkaRecordRequest(
    val id: Long,
    val topic: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val executionTime: LocalDateTime,
)

@Serializable
data class DeleteKafkaRecordRequest(
    val id: Long,
    val topic: String,
)

@Serializable
data class KafkaConsumerRecordDto(
    val id: Long,
    val topic: String,
    val partition: Int,
    val recordOffset: Long,
    val retries: Int,
    val lastRetry: String?,
    val key: String?,
    val value: String?,
    val headersJson: String?,
    val recordTimestamp: String?,
    val createdAt: String,
)
