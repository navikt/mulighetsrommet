package no.nav.mulighetsrommet.arena.adapter

import kotlinx.serialization.json.Json
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider
import no.nav.common.kafka.consumer.KafkaConsumerClient
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRecordProcessor
import no.nav.common.kafka.consumer.feilhandtering.util.KafkaConsumerRecordProcessorBuilder
import no.nav.common.kafka.consumer.util.ConsumerUtils.findConsumerConfigsWithStoreOnFailure
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import no.nav.mulighetsrommet.arena.adapter.consumers.TiltakEndretConsumer
import no.nav.mulighetsrommet.arena.adapter.consumers.TiltakdeltakerEndretConsumer
import no.nav.mulighetsrommet.arena.adapter.consumers.TiltakgjennomforingEndretConsumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import java.util.*
import java.util.function.Consumer

class Kafka(
    config: KafkaConfig,
    consumerPreset: Properties,
    private val db: Database,
    client: MulighetsrommetApiClient
) {

    private val logger = LoggerFactory.getLogger(Kafka::class.java)
    private val consumerClient: KafkaConsumerClient
    private val consumerRecordProcessor: KafkaConsumerRecordProcessor
    private val consumerTopics: Map<String, String> = config.topics.consumer

    private val tiltakEndretConsumer = TiltakEndretConsumer(client)
    private val tiltakgjennomforingEndretConsumer = TiltakgjennomforingEndretConsumer(client)
    private val tiltakdeltakerEndretConsumer = TiltakdeltakerEndretConsumer(client)

    init {
        logger.debug("Initializing Kafka")

        val kafkaConsumerRepository = KafkaConsumerRepository(db)
        val consumerTopicsConfig = configureConsumersTopics(kafkaConsumerRepository)
        val lockProvider = JdbcLockProvider(db.dataSource)

        consumerClient = KafkaConsumerClientBuilder.builder()
            .withProperties(consumerPreset)
            .withTopicConfigs(consumerTopicsConfig)
            .build()

        consumerRecordProcessor = KafkaConsumerRecordProcessorBuilder
            .builder()
            .withLockProvider(lockProvider)
            .withKafkaConsumerRepository(kafkaConsumerRepository)
            .withConsumerConfigs(findConsumerConfigsWithStoreOnFailure(consumerTopicsConfig))
            .build()
    }

    fun startTopicConsumption() {
        logger.debug("Starting kafka consumer client")
        consumerClient.start()

        logger.debug("Starting kafka consumer record processor")
        consumerRecordProcessor.start()
    }

    fun stopTopicConsumption() {
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
            consumerTopics["tiltakendret"] -> tiltakEndretConsumer.process(payload)
            consumerTopics["tiltakgjennomforingendret"] -> tiltakgjennomforingEndretConsumer.process(payload)
            consumerTopics["tiltakdeltakerendret"] -> tiltakdeltakerEndretConsumer.process(payload)
            else -> logger.info("Klarte ikke å mappe topic. Ukjent topic: $topic")
        }
    }
}
