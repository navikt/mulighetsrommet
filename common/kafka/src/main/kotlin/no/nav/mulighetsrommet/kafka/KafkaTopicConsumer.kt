package no.nav.mulighetsrommet.kafka

import org.apache.kafka.common.serialization.Deserializer
import java.util.*

abstract class KafkaTopicConsumer<K, V>(
    private val config: Config,
    val keyDeserializer: Deserializer<K>,
    val valueDeserializer: Deserializer<V>,
) {

    data class Config(
        val id: String,
        val topic: String,
        val consumerProperties: Properties,
    )

    fun getConsumerId(): String = config.id

    fun getConsumerTopic(): String = config.topic

    fun getConsumerProperties(): Properties = config.consumerProperties

    abstract suspend fun consume(key: K, message: V)
}
