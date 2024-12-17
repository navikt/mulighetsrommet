package no.nav.mulighetsrommet.kafka

import kotlinx.coroutines.runBlocking
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider
import no.nav.common.kafka.consumer.KafkaConsumerClient
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRecordProcessor
import no.nav.common.kafka.consumer.feilhandtering.util.KafkaConsumerRecordProcessorBuilder
import no.nav.common.kafka.consumer.util.ConsumerUtils.findConsumerConfigsWithStoreOnFailure
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder.TopicConfig
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.metrics.Metrikker
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.slf4j.LoggerFactory
import java.util.*
import java.util.function.Consumer

class KafkaConsumerOrchestrator(
    config: Config = Config(),
    consumerPreset: Properties,
    db: Database,
    consumers: List<KafkaTopicConsumer<*, *>>,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val consumerClients: Map<String, Consumer>
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

        resetTopics(config, consumers)

        consumerClients = consumers.associate { consumer ->
            val topicConfig = toTopicConfig(consumer)
            val client = toKafkaConsumerClient(consumer, consumerPreset, topicConfig)
            consumer.getConsumerId() to Consumer(topicConfig, client)
        }

        val topicConfigs = consumerClients.map { it.value.topicConfig }
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
        return consumerClients.map { it.value.client }
    }

    fun updateRunningTopics(topics: List<Topic>): List<Topic> {
        val current = getTopics()
        topicRepository.updateRunning(topics)
        return getUpdatedTopicsOnly(topics, current)
    }

    fun stopPollingTopicChanges() {
        topicPoller.stop()
    }

    private fun <K, V> toTopicConfig(consumer: KafkaTopicConsumer<K, V>): TopicConfig<K, V> {
        return TopicConfig<K, V>()
            .withMetrics(Metrikker.appMicrometerRegistry)
            .withLogging()
            .withStoreOnFailure(kafkaConsumerRepository)
            .withConsumerConfig(
                consumer.getConsumerTopic(),
                consumer.keyDeserializer,
                consumer.valueDeserializer,
                Consumer { event ->
                    runBlocking {
                        consumer.consume(event.key(), event.value())
                    }
                },
            )
    }

    private fun toKafkaConsumerClient(
        consumer: KafkaTopicConsumer<*, *>,
        consumerPreset: Properties,
        topicConfig: TopicConfig<out Any?, out Any?>,
    ): KafkaConsumerClient {
        fun withConsumerGroupId(p: Properties, consumerGroupId: String): Properties {
            val p2 = Properties()
            p2.putAll(p)
            p2[ConsumerConfig.GROUP_ID_CONFIG] = consumerGroupId
            return p2
        }

        val consumerClientPreset = consumer.getConsumerGroupId()
            ?.let { withConsumerGroupId(consumerPreset, it) }
            ?: consumerPreset

        return KafkaConsumerClientBuilder.builder()
            .withProperties(consumerClientPreset)
            .withTopicConfig(topicConfig)
            .build()
    }

    private fun updateClientRunningState() {
        getTopics().forEach {
            val client = consumerClients[it.id]?.client
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

    private fun resetTopics(config: Config, consumers: List<KafkaTopicConsumer<*, *>>) {
        val currentTopics = topicRepository.getAll()

        val topics = consumers.map { consumer ->
            val id = consumer.getConsumerId()

            val running = currentTopics.firstOrNull { it.id == id }
                ?.running
                ?: config.consumerInitialRunningState

            Topic(id = id, topic = consumer.getConsumerTopic(), type = TopicType.CONSUMER, running = running)
        }
        topicRepository.setAll(topics)
    }

    private fun getUpdatedTopicsOnly(updated: List<Topic>, current: List<Topic>) = updated.filter { x ->
        current.any { y -> y.id == x.id && y.running != x.running }
    }
}

private fun validateConsumers(consumers: List<KafkaTopicConsumer<*, *>>) {
    require(consumers.distinctBy { it.getConsumerId() }.size == consumers.size) {
        "Each consumer must have a unique 'id'. At least two consumers share the same 'id'."
    }
}
