package no.nav.mulighetsrommet.kafka

import org.apache.kafka.common.serialization.Deserializer

abstract class KafkaTopicConsumer<K, V>(
    val config: Config,
    val keyDeserializer: Deserializer<K>,
    val valueDeserializer: Deserializer<V>,
) {

    data class Config(
        val id: String,
        val topic: String,
        val initialRunningState: Boolean = false,
        val consumerGroupId: String? = null,
    )

    abstract suspend fun consume(key: K, message: V)
}
