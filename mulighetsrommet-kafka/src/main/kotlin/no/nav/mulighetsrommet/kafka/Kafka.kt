package no.nav.mulighetsrommet.kafka

import io.ktor.client.*
import kotlinx.serialization.json.Json
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider
import no.nav.common.kafka.consumer.KafkaConsumerClient
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRecordProcessor
import no.nav.common.kafka.consumer.feilhandtering.util.KafkaConsumerRecordProcessorBuilder
import no.nav.common.kafka.consumer.util.ConsumerUtils.findConsumerConfigsWithStoreOnFailure
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import no.nav.mulighetsrommet.kafka.consumers.TiltakEndretConsumer
import no.nav.mulighetsrommet.kafka.consumers.TiltakgjennomforingEndretConsumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import java.util.*
import java.util.function.Consumer


class Kafka(config: KafkaConfig, consumerPreset: Properties, private val db: Database, client: MulighetsrommetApiClient) {

    private val logger = LoggerFactory.getLogger(Kafka::class.java)
    private val consumerClient: KafkaConsumerClient
    private val consumerRecordProcessor: KafkaConsumerRecordProcessor
    private val consumerTopics: Map<String, String> = config.topics.consumer

    private val tiltakEndretConsumer = TiltakEndretConsumer(client)
    private val tiltakgjennomforingEndretConsumer = TiltakgjennomforingEndretConsumer(client)


    init {
        logger.debug("Initializing Kafka")

        val kafkaConsumerRepository = KafkaConsumerRepository(db)
        val consumerTopicsConfig = configureConsumersTopics(kafkaConsumerRepository)
        val lockProvider = JdbcLockProvider(db.dataSource)

        consumerClient = KafkaConsumerClientBuilder.builder()
            .withProperties(consumerPreset)
            .withTopicConfigs(consumerTopicsConfig)
            .build()

        logger.debug("Starting kafka consumer client")
        consumerClient.start()

        consumerRecordProcessor = KafkaConsumerRecordProcessorBuilder
            .builder()
            .withLockProvider(lockProvider)
            .withKafkaConsumerRepository(kafkaConsumerRepository)
            .withConsumerConfigs(findConsumerConfigsWithStoreOnFailure(consumerTopicsConfig))
            .build()

        consumerRecordProcessor.start()
        consumerRecordProcessor.close()
        logger.debug("Starting kafka consumer record processor")
    }

    fun stopClient() {
        consumerClient.stop()
        consumerRecordProcessor.close()
        logger.debug("Stopped kafka clients and processors")
    }

    private fun configureConsumersTopics(repository: KafkaConsumerRepository): List<KafkaConsumerClientBuilder.TopicConfig<String, String>> {
        return consumerTopics.map { topic ->
            KafkaConsumerClientBuilder.TopicConfig<String, String>()
                .withStoreOnFailure(repository)
                .withLogging()
                .withConsumerConfig(
                    topic.value,
                    stringDeserializer(),
                    stringDeserializer(),
                    Consumer<ConsumerRecord<String, String>> {
                        db.persistKafkaEvent(it.topic(), it.key(), it.offset(), it.value())
                        topicMapper(it.topic(), it.value())
                    }
                )
        }
    }

    private fun topicMapper(topic: String, value: String) {
        val payload = Json.parseToJsonElement(value)
        when (topic) {
            consumerTopics.get("tiltakendret") -> tiltakEndretConsumer.process(payload)
            consumerTopics.get("tiltakgjennomforingendret") -> tiltakgjennomforingEndretConsumer.process(payload)
            else -> logger.info("Klarte ikke å mappe topic. Ukjent topic: $topic")
        }
    }
}
