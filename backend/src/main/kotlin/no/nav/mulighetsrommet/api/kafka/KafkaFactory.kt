package no.nav.mulighetsrommet.api.kafka

import com.typesafe.config.ConfigFactory
import io.ktor.config.HoconApplicationConfig
import no.nav.common.kafka.consumer.KafkaConsumerClient
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import no.nav.common.kafka.util.KafkaPropertiesPreset
import no.nav.mulighetsrommet.api.database.DatabaseFactory
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.slf4j.LoggerFactory
import java.util.Properties
import java.util.function.Consumer

class KafkaFactory(private val db: DatabaseFactory) {

    private val logger = LoggerFactory.getLogger(KafkaFactory::class.java)
    private val appConfig = HoconApplicationConfig(ConfigFactory.load())
    private val consumerClient: KafkaConsumerClient

    init {
        logger.debug("Initializing KafkaFactory.")

        val consumerProperties = configureProperties()
        val topics = configureTopics()

        consumerClient = KafkaConsumerClientBuilder.builder()
            .withProperties(consumerProperties)
            .withTopicConfigs(topics)
            .build()

        consumerClient.start()

        logger.debug("Consumer client started. Done with initializing KafkaFactory.")
    }

    fun stopClient() {
        consumerClient.stop()
    }

    private fun configureProperties(): Properties {
        val consumerGroupId = appConfig.property("ktor.kafka.consumerGroupId").getString()
        val kafkaBrokers = appConfig.property("ktor.kafka.kafkaBrokers").getString()
        return if (appConfig.property("ktor.localDevelopment").getString() == "true") {
            KafkaPropertiesBuilder.consumerBuilder()
                .withBrokerUrl(kafkaBrokers)
                .withBaseProperties()
                .withConsumerGroupId(consumerGroupId)
                .withDeserializers(ByteArrayDeserializer::class.java, ByteArrayDeserializer::class.java)
                .build()
        } else {
            KafkaPropertiesPreset.aivenDefaultConsumerProperties(consumerGroupId)
        }
    }

    private fun configureTopics(): List<KafkaConsumerClientBuilder.TopicConfig<String, String>> {
        return KafkaTopics.values().map { it ->
            KafkaConsumerClientBuilder.TopicConfig<String, String>()
                .withLogging()
                .withConsumerConfig(
                    it.topic,
                    stringDeserializer(),
                    stringDeserializer(),
                    Consumer<ConsumerRecord<String, String>> { logTopicContent(it) }
                )
        }
    }

    // Temporary print out until we actually implement something with the events.
    private fun logTopicContent(consumerRecord: ConsumerRecord<String, String>) {
        logger.debug("Topic: ${consumerRecord.topic()} - Value: ${consumerRecord.value()}")
    }
}
