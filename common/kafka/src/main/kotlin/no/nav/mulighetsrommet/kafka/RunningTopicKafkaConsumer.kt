package no.nav.mulighetsrommet.kafka

import no.nav.common.kafka.consumer.ConsumeStatus
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory

/**
 * Wraps a [KafkaTopicConsumer] so that records are only forwarded to [delegate] when [isRunning] returns true.
 *
 * This exists to guard against [no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRecordProcessor], which
 * replays previously failed/stored records directly against the registered [no.nav.common.kafka.consumer.TopicConsumer],
 * bypassing the [Topic.running] state that otherwise starts/stops the live [no.nav.common.kafka.consumer.KafkaConsumerClient].
 * Without this wrapper, a paused topic consumer could still have the stored record processed.
 */
internal class RunningTopicKafkaConsumer<K, V>(
    private val id: String,
    private val isRunning: () -> Boolean,
    private val delegate: KafkaTopicConsumer<K, V>,
) : KafkaTopicConsumer<K, V>(delegate.keyDeserializer, delegate.valueDeserializer) {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun consume(record: ConsumerRecord<K, V>): ConsumeStatus {
        if (!isRunning()) {
            logger.info("Skipping consumption of record for consumer '$id' because its topic is not running")
            return ConsumeStatus.OK
        }
        return delegate.consume(record)
    }

    override suspend fun consume(key: K, message: V) {
        delegate.consume(key, message)
    }
}
