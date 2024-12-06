package no.nav.mulighetsrommet.kafka

import org.apache.kafka.common.serialization.Deserializer

abstract class KafkaTopicConsumer<K, V>(
    private val config: Config,
    val keyDeserializer: Deserializer<K>,
    val valueDeserializer: Deserializer<V>,
) {

    data class Config(
        val id: String,
        val topic: String,
        val consumerGroupId: String? = null,
    )

    fun getConsumerId(): String = config.id

    fun getConsumerGroupId(): String? = config.consumerGroupId

    fun getConsumerTopic(): String = config.topic

    abstract suspend fun consume(key: K, message: V)
}
