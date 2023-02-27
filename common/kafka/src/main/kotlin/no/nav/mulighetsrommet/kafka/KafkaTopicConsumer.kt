package no.nav.mulighetsrommet.kafka

import kotlinx.coroutines.runBlocking
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder
import org.apache.kafka.common.serialization.Deserializer
import java.util.function.Consumer

abstract class KafkaTopicConsumer<K, V>(
    val config: Config,
    private val keyDeserializer: Deserializer<K>,
    private val valueDeserializer: Deserializer<V>
) {

    data class Config(
        val id: String,
        val topic: String,
        val initialRunningState: Boolean = false,
    )

    internal fun toTopicConfig(kafkaConsumerRepository: KafkaConsumerRepository): KafkaConsumerClientBuilder.TopicConfig<K, V> {
        return KafkaConsumerClientBuilder.TopicConfig<K, V>()
            .withLogging()
            .withStoreOnFailure(kafkaConsumerRepository)
            .withConsumerConfig(
                config.topic,
                keyDeserializer,
                valueDeserializer,
                Consumer { event ->
                    runBlocking {
                        consume(event.key(), event.value())
                    }
                }
            )
    }

    abstract suspend fun consume(key: K, message: V)
}
