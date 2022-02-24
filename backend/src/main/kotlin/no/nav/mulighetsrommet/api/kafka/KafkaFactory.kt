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
    private val kafkaConfig = HoconApplicationConfig(ConfigFactory.load()).config("ktor.kafka")
    private val consumerClient: KafkaConsumerClient
    private val topicMap: Map<String, String>

    init {
        logger.debug("Initializing KafkaFactory.")

        topicMap = getConsumerTopics()

        val consumerProperties = configureProperties()
        val consumerTopics = configureConsumersTopics()

        consumerClient = KafkaConsumerClientBuilder.builder()
            .withProperties(consumerProperties)
            .withTopicConfigs(consumerTopics)
            .build()

        consumerClient.start()

        logger.debug("Consumer client started. Done with initializing KafkaFactory.")
    }

    fun stopClient() {
        consumerClient.stop()
    }

    private fun configureProperties(): Properties {
        val consumerGroupId = kafkaConfig.property("consumerGroupId").getString()
        val kafkaBrokers = kafkaConfig.property("kafkaBrokers").getString()
        val isLocalDevelopment = HoconApplicationConfig(ConfigFactory.load()).property("ktor.localDevelopment").getString()
        // // TODO: Discuss if we really need a local setup of kafka or not
        return if (isLocalDevelopment == "true") {
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

    // TODO: Kanskje finne en bedre måte. ApplicationConfig støtter ikke å iterere over keys, noe som er tullete
    fun getConsumerTopics(): Map<String, String> {
        val configTopics = kafkaConfig.config("topics.consumer")
        return mapOf<String, String>(
            // Pair("tiltakgjennomforingendret", configTopics.property("tiltakgjennomforingendret").getString()),
            // Pair("tiltakdeltakerendret", configTopics.property("tiltakdeltakeredret").getString()),
            // Pair("tiltaksgruppeendret", configTopics.property("tiltaksgruppeendret").getString()),
            Pair("tiltakendret", configTopics.property("tiltakendret").getString()),
            // Pair("avtaleinfoendret", configTopics.property("avtaleinfoendret").getString())
        )
    }

    private fun configureConsumersTopics(): List<KafkaConsumerClientBuilder.TopicConfig<String, String>> {
        return topicMap.values.map { topic ->
            val eventProcessor = EventProcessor(db, topicMap)
            KafkaConsumerClientBuilder.TopicConfig<String, String>()
                .withLogging()
                .withConsumerConfig(
                    topic,
                    stringDeserializer(),
                    stringDeserializer(),
                    Consumer<ConsumerRecord<String, String>> { eventProcessor.process(it) }
                )
        }
    }
}
