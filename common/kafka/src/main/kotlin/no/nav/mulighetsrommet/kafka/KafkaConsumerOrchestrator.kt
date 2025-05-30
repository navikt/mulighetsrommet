package no.nav.mulighetsrommet.kafka

import kotlinx.coroutines.runBlocking
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider
import no.nav.common.kafka.consumer.KafkaConsumerClient
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRecordProcessor
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRepository
import no.nav.common.kafka.consumer.feilhandtering.StoredConsumerRecord
import no.nav.common.kafka.consumer.feilhandtering.util.KafkaConsumerRecordProcessorBuilder
import no.nav.common.kafka.consumer.util.ConsumerUtils.findConsumerConfigsWithStoreOnFailure
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder.TopicConfig
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.metrics.Metrikker
import org.slf4j.LoggerFactory
import java.util.*
import java.util.function.Consumer

class KafkaConsumerOrchestrator(
    config: Config = Config(),
    db: Database,
    consumers: Map<KafkaTopicConsumer.Config, KafkaTopicConsumer<*, *>>,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val consumersById: Map<String, Consumer>
    private val consumerRecordProcessor: KafkaConsumerRecordProcessor
    private val topicPoller: Poller
    private val topicRepository = TopicRepository(db)
    private val kafkaConsumerRepository = KafkaConsumerRepositoryImpl(db)

    data class Config(
        /**
         * Whether consumer clients starts in a running state or not.
         */
        val consumerInitialRunningState: Boolean = false,

        /**
         * Frequency in milliseconds of how often the [Topic.running] state should be polled.
         */
        val consumerRunningStatePollDelay: Long = 10_000,
    )

    private data class Consumer(
        val topicConfig: TopicConfig<*, *>,
        val client: KafkaConsumerClient,
    )

    init {
        logger.info("Initializing Kafka consumer clients")

        validateConsumers(consumers)

        resetTopics(config, consumers.keys)

        consumersById = consumers.entries.associate { (config, consumer) ->
            config.id to createConsumer(config, consumer)
        }

        val topicConfigs = consumersById.values.map { it.topicConfig }
        consumerRecordProcessor = KafkaConsumerRecordProcessorBuilder
            .builder()
            .withLockProvider(JdbcLockProvider(db.getDatasource()))
            .withKafkaConsumerRepository(kafkaConsumerRepository)
            .withConsumerConfigs(findConsumerConfigsWithStoreOnFailure(topicConfigs))
            .build()

        topicPoller = Poller(config.consumerRunningStatePollDelay) {
            updateClientRunningState()
        }

        topicPoller.start()
    }

    fun enableFailedRecordProcessor() {
        consumerRecordProcessor.start()
        logger.info("Started kafka consumer record processor")
    }

    fun disableFailedRecordProcessor() {
        consumerRecordProcessor.stop()
        logger.info("Stopped kafka processors")
    }

    fun getTopics(): List<Topic> {
        return topicRepository.getAll()
    }

    fun getConsumers(): List<KafkaConsumerClient> {
        return consumersById.map { it.value.client }
    }

    fun updateRunningTopics(topics: List<Topic>): List<Topic> {
        val current = getTopics()
        topicRepository.updateRunning(topics)
        return getUpdatedTopicsOnly(topics, current)
    }

    fun stopPollingTopicChanges() {
        topicPoller.stop()
    }

    fun getAllStoredConsumerRecords(): MutableList<StoredConsumerRecord> {
        return kafkaConsumerRepository.getAll()
    }

    private fun createConsumer(
        config: KafkaTopicConsumer.Config,
        consumer: KafkaTopicConsumer<*, *>,
    ): KafkaConsumerOrchestrator.Consumer {
        val topicConfig = toTopicConfig(config.topic, consumer, kafkaConsumerRepository)
        val client = toKafkaConsumerClient(config.consumerProperties, topicConfig)
        return Consumer(topicConfig, client)
    }

    private fun updateClientRunningState() {
        getTopics().forEach {
            val client = consumersById[it.id]?.client
            if (client != null) {
                if (client.isRunning && !it.running) {
                    client.stop()
                    logger.info("Stopped client for topic ${it.topic}")
                } else if (!client.isRunning && it.running) {
                    client.start()
                    logger.info("Started client for topic ${it.topic}")
                }
            }
        }
    }

    private fun resetTopics(config: Config, consumerConfigs: Set<KafkaTopicConsumer.Config>) {
        val currentTopics = topicRepository.getAll()

        val topics = consumerConfigs.map { consumerConfig ->
            val id = consumerConfig.id
            val topic = consumerConfig.topic

            val running = currentTopics.firstOrNull { it.id == id }
                ?.running
                ?: config.consumerInitialRunningState

            Topic(id = id, topic = topic, type = TopicType.CONSUMER, running = running)
        }
        topicRepository.setAll(topics)
    }

    private fun getUpdatedTopicsOnly(updated: List<Topic>, current: List<Topic>) = updated.filter { x ->
        current.any { y -> y.id == x.id && y.running != x.running }
    }
}

private fun validateConsumers(consumers: Map<KafkaTopicConsumer.Config, KafkaTopicConsumer<*, *>>) {
    require(consumers.keys.distinctBy { it.id }.size == consumers.size) {
        "Each consumer must have a unique 'id'. At least two consumers share the same 'id'."
    }
}

private fun toKafkaConsumerClient(
    consumerProperties: Properties,
    topicConfig: TopicConfig<out Any?, out Any?>,
): KafkaConsumerClient {
    return KafkaConsumerClientBuilder.builder()
        .withProperties(consumerProperties)
        .withTopicConfig(topicConfig)
        .build()
}

private fun <K, V> toTopicConfig(
    topic: String,
    consumer: KafkaTopicConsumer<K, V>,
    consumerRecordRepository: KafkaConsumerRepository,
): TopicConfig<K, V> {
    return TopicConfig<K, V>()
        .withMetrics(Metrikker.appMicrometerRegistry)
        .withLogging()
        .withStoreOnFailure(consumerRecordRepository)
        .withConsumerConfig(
            topic,
            consumer.keyDeserializer,
            consumer.valueDeserializer,
            Consumer { event ->
                runBlocking {
                    consumer.consume(event.key(), event.value())
                }
            },
        )
}
