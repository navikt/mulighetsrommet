package no.nav.mulighetsrommet.arena.adapter

import kotlinx.serialization.json.Json
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider
import no.nav.common.kafka.consumer.KafkaConsumerClient
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRecordProcessor
import no.nav.common.kafka.consumer.feilhandtering.util.KafkaConsumerRecordProcessorBuilder
import no.nav.common.kafka.consumer.util.ConsumerUtils.findConsumerConfigsWithStoreOnFailure
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import no.nav.mulighetsrommet.arena.adapter.consumers.SakEndretConsumer
import no.nav.mulighetsrommet.arena.adapter.consumers.TiltakEndretConsumer
import no.nav.mulighetsrommet.arena.adapter.consumers.TiltakdeltakerEndretConsumer
import no.nav.mulighetsrommet.arena.adapter.consumers.TiltakgjennomforingEndretConsumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import java.util.*
import java.util.function.Consumer

class KafkaConsumerOrchestrator(
    config: KafkaConfig,
    consumerPreset: Properties,
    private val db: Database,
    private val tiltakEndretConsumer: TiltakEndretConsumer,
    private val tiltakgjennomforingEndretConsumer: TiltakgjennomforingEndretConsumer,
    private val tiltakdeltakerEndretConsumer: TiltakdeltakerEndretConsumer,
    private val sakEndretConsumer: SakEndretConsumer
) {

    private val logger = LoggerFactory.getLogger(KafkaConsumerOrchestrator::class.java)
    private val consumerClient: KafkaConsumerClient
    private val consumerRecordProcessor: KafkaConsumerRecordProcessor
    private val consumerTopics: Map<String, String> = config.topics.consumer

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

    fun enableTopicConsumption() {
        consumerClient.start()
        logger.debug("Started kafka consumer client")
    }

    fun enableFailedRecordProcessor() {
        consumerRecordProcessor.start()
        logger.debug("Started kafka consumer record processor")
    }

    fun disableTopicConsumption() {
        consumerClient.stop()
        logger.debug("Stopped kafka consumer client")
    }

    fun disableFailedRecordProcessor() {
        consumerRecordProcessor.close()
        logger.debug("Stopped kafka processors")
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
            consumerTopics["sakendret"] -> sakEndretConsumer.process(payload)
            else -> logger.info("Klarte ikke å mappe topic. Ukjent topic: $topic")
        }
    }
}
