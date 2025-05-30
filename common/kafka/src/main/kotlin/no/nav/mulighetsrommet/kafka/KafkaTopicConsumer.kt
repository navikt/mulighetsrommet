package no.nav.mulighetsrommet.kafka

import org.apache.kafka.common.serialization.Deserializer
import java.util.*

abstract class KafkaTopicConsumer<K, V>(
    val keyDeserializer: Deserializer<K>,
    val valueDeserializer: Deserializer<V>,
) {

    data class Config(
        val id: String,
        val topic: String,
        val consumerProperties: Properties,
    )

    abstract suspend fun consume(key: K, message: V)
}
