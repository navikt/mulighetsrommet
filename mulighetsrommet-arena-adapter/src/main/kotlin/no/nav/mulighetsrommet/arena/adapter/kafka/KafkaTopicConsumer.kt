package no.nav.mulighetsrommet.arena.adapter.kafka

import kotlinx.coroutines.runBlocking
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder
import no.nav.mulighetsrommet.arena.adapter.ConsumerConfig
import org.apache.kafka.common.serialization.Deserializer
import java.util.function.Consumer

abstract class KafkaTopicConsumer<K, V>(
    val config: ConsumerConfig,
    private val keyDeserializer: Deserializer<K>,
    private val valueDeserializer: Deserializer<V>
) {
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
                        run(event.value())
                    }
                }
            )
    }

    abstract suspend fun run(event: V)
}
