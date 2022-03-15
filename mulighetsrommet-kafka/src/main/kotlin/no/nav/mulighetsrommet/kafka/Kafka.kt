package no.nav.mulighetsrommet.kafka

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import no.nav.common.kafka.consumer.KafkaConsumerClient
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import no.nav.mulighetsrommet.kafka.consumers.TiltakEndretConsumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import java.util.*
import java.util.function.Consumer

class Kafka(config: KafkaConfig, consumerPreset: Properties, private val db: Database) {

    private val logger = LoggerFactory.getLogger(Kafka::class.java)
    private val consumerClient: KafkaConsumerClient
    private val consumerTopics: Map<String, String> = config.topics.consumer

    init {
        logger.debug("Initializing KafkaFactory.")

        val consumerTopicsConfig = configureConsumersTopics()

        consumerClient = KafkaConsumerClientBuilder.builder()
            .withProperties(consumerPreset)
            .withTopicConfigs(consumerTopicsConfig)
            .build()

        consumerClient.start()

        logger.debug("Consumer client started. Done with initializing KafkaFactory.")
    }

    fun stopClient() {
        consumerClient.stop()
    }

    private fun configureConsumersTopics(): List<KafkaConsumerClientBuilder.TopicConfig<String, String>> {
        return consumerTopics.map { topic ->
            KafkaConsumerClientBuilder.TopicConfig<String, String>()
                .withLogging()
                .withConsumerConfig(
                    topic.value,
                    stringDeserializer(),
                    stringDeserializer(),
                    Consumer<ConsumerRecord<String, String>> {
                        val payload = Json.parseToJsonElement(it.value())
                        db.persistKafkaEvent(it.topic(), it.key(), it.offset(), payload)
                        topicMapper(it.topic(), payload)
                    }
                )
        }
    }

    private fun topicMapper(topic: String, payload: JsonElement) {
        when (topic) {
            consumerTopics.get("tiltakendret") -> TiltakEndretConsumer.process(payload)
            else -> logger.info("Klarte ikke å mappe topic. Ukjent topic: $topic")
        }
    }
}
