package no.nav.mulighetsrommet.arena.adapter.kafka

import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider
import no.nav.common.kafka.consumer.KafkaConsumerClient
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRecordProcessor
import no.nav.common.kafka.consumer.feilhandtering.util.KafkaConsumerRecordProcessorBuilder
import no.nav.common.kafka.consumer.util.ConsumerUtils.findConsumerConfigsWithStoreOnFailure
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder
import no.nav.mulighetsrommet.arena.adapter.repositories.Topic
import no.nav.mulighetsrommet.arena.adapter.repositories.TopicRepository
import no.nav.mulighetsrommet.arena.adapter.repositories.TopicType
import no.nav.mulighetsrommet.database.Database
import org.slf4j.LoggerFactory
import java.util.*

class KafkaConsumerOrchestrator(
    consumerPreset: Properties,
    config: Config,
    db: Database,
    consumers: List<KafkaTopicConsumer<*, *>>,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val consumerClients: Map<String, KafkaConsumerClient>
    private val consumerRecordProcessor: KafkaConsumerRecordProcessor
    private val topicPoller: Poller
    private val topicRepository = TopicRepository(db)
    private val kafkaConsumerRepository = KafkaConsumerRepository(db)

    data class Config(
        val topicStatePollDelay: Long,
    )

    init {
        logger.info("Initializing Kafka consumer clients")

        updateTopics(consumers)

        val consumerTopicsConfig = consumers.map { consumer ->
            consumer.toTopicConfig(kafkaConsumerRepository)
        }
        consumerClients = consumerTopicsConfig.associate {
            val client = KafkaConsumerClientBuilder.builder()
                .withProperties(consumerPreset)
                .withTopicConfig(it)
                .build()
            it.consumerConfig.topic to client
        }

        val lockProvider = JdbcLockProvider(db.getDatasource())
        consumerRecordProcessor = KafkaConsumerRecordProcessorBuilder
            .builder()
            .withLockProvider(lockProvider)
            .withKafkaConsumerRepository(kafkaConsumerRepository)
            .withConsumerConfigs(findConsumerConfigsWithStoreOnFailure(consumerTopicsConfig))
            .build()

        updateClientRunningState()

        topicPoller = Poller(config.topicStatePollDelay) {
            updateClientRunningState()
        }

        topicPoller.start()
    }

    fun enableFailedRecordProcessor() {
        consumerRecordProcessor.start()
        logger.info("Started kafka consumer record processor")
    }

    fun disableFailedRecordProcessor() {
        consumerRecordProcessor.close()
        logger.info("Stopped kafka processors")
    }

    fun getTopics() = topicRepository.selectAll()

    fun getConsumers(): List<KafkaConsumerClient> {
        return consumerClients.toList().map { it.second }
    }

    fun updateRunningTopics(topics: List<Topic>): List<Topic> {
        val current = getTopics()
        topicRepository.updateRunning(topics)
        return getUpdatedTopicsOnly(topics, current)
    }

    fun stopPollingTopicChanges() = topicPoller.stop()

    private fun updateClientRunningState() {
        getTopics().forEach {
            val client = consumerClients[it.topic]
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

    private fun updateTopics(consumers: List<KafkaTopicConsumer<*, *>>) {
        val currentTopics = topicRepository.selectAll()

        val topics = consumers.map { consumer ->
            val (id, topic, initialRunningState) = consumer.config

            val running = currentTopics
                .firstOrNull { it.id == id }
                .let { it?.running ?: initialRunningState }

            Topic(
                id = id,
                topic = topic,
                type = TopicType.CONSUMER,
                running = running
            )
        }
        topicRepository.upsertTopics(topics)
    }

    private fun getUpdatedTopicsOnly(updated: List<Topic>, current: List<Topic>) =
        updated.filter { x -> current.any { y -> y.id == x.id && y.running != x.running } }
}
