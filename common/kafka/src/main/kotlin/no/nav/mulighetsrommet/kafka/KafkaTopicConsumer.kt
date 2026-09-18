package no.nav.mulighetsrommet.kafka

import kotlinx.coroutines.runBlocking
import no.nav.common.kafka.consumer.ConsumeStatus
import no.nav.common.kafka.consumer.TopicConsumer
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRepository
import no.nav.common.kafka.consumer.util.ConsumerUtils
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import java.time.Instant
import java.util.Properties

abstract class KafkaTopicConsumer<K, V>(
    val keyDeserializer: Deserializer<K>,
    val valueDeserializer: Deserializer<V>,
) : TopicConsumer<K, V> {

    data class Config(
        val id: String,
        val topic: String,
        val consumerProperties: Properties,
    )

    /**
     * Override this if you need access to the full [ConsumerRecord], e.g. to read headers before
     * deciding whether to process the message. The default implementation dispatches to
     * [consume] (key, message).
     */
    override fun consume(record: ConsumerRecord<K, V>): ConsumeStatus = runBlocking {
        consume(record.key(), record.value())
        ConsumeStatus.OK
    }

    /**
     * Override this for simple key/value based processing when you don't need access to the
     * record's headers or other metadata.
     */
    open suspend fun consume(key: K, message: V): Unit = throw NotImplementedError(
        "${javaClass.simpleName} must override either consume(record) or consume(key, message)",
    )
}

abstract class ScheduledMessageKafkaTopicConsumer<K, V>(
    private val kafkaConsumerRepository: KafkaConsumerRepository,
    private val keySerde: Serde<K>,
    private val valueSerde: Serde<V>,
) : KafkaTopicConsumer<K, V>(keySerde.deserializer(), valueSerde.deserializer()) {

    override fun consume(record: ConsumerRecord<K, V>): ConsumeStatus {
        val scheduledAt = record.headers()
            .firstOrNull { it.key() == KAFKA_CONSUMER_RECORD_PROCESSOR_SCHEDULED_AT }
            ?.let { Instant.parse(String(it.value())) }

        if (scheduledAt != null && Instant.now().isBefore(scheduledAt)) {
            kafkaConsumerRepository.storeRecord(
                ConsumerUtils.mapToStoredRecord(
                    record,
                    keySerde.serializer(),
                    valueSerde.serializer(),
                ),
            )
            return ConsumeStatus.OK
        }

        return runBlocking {
            consume(record.key(), record.value())
            ConsumeStatus.OK
        }
    }

    abstract override suspend fun consume(key: K, message: V)
}
