package no.nav.mulighetsrommet.arena.adapter.kafka

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider
import no.nav.common.kafka.consumer.KafkaConsumerClient
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRecordProcessor
import no.nav.common.kafka.consumer.feilhandtering.util.KafkaConsumerRecordProcessorBuilder
import no.nav.common.kafka.consumer.util.ConsumerUtils.findConsumerConfigsWithStoreOnFailure
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import no.nav.mulighetsrommet.arena.adapter.repositories.Topic
import no.nav.mulighetsrommet.arena.adapter.repositories.TopicRepository
import no.nav.mulighetsrommet.arena.adapter.repositories.TopicType
import no.nav.mulighetsrommet.database.Database
import org.slf4j.LoggerFactory
import java.util.*
import java.util.function.Consumer

class KafkaConsumerOrchestrator(
    consumerPreset: Properties,
    db: Database,
    private val group: ConsumerGroup,
    private val topicRepository: TopicRepository,
    pollDelay: Long
) {

    private val logger = LoggerFactory.getLogger(KafkaConsumerOrchestrator::class.java)
    private val consumerClients: Map<String, KafkaConsumerClient>
    private val consumerRecordProcessor: KafkaConsumerRecordProcessor
    private val topicPoller: Poller

    init {
        logger.info("Initializing Kafka consumer clients")

        updateTopics(group.consumers)

        val kafkaConsumerRepository = KafkaConsumerRepository(db)
        val consumerTopicsConfig = configureConsumersTopics(kafkaConsumerRepository)
        val lockProvider = JdbcLockProvider(db.getDatasource())

        consumerClients = mutableMapOf()
        consumerTopicsConfig.forEach {
            val client = KafkaConsumerClientBuilder.builder()
                .withProperties(consumerPreset)
                .withTopicConfig(it)
                .build()
            consumerClients.put(it.consumerConfig.topic, client)
        }

        consumerRecordProcessor = KafkaConsumerRecordProcessorBuilder
            .builder()
            .withLockProvider(lockProvider)
            .withKafkaConsumerRepository(kafkaConsumerRepository)
            .withConsumerConfigs(findConsumerConfigsWithStoreOnFailure(consumerTopicsConfig))
            .build()

        startConsumerClients()

        topicPoller = Poller(pollDelay) {
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

    private fun updateTopics(consumers: List<TopicConsumer<*>>) {
        val currentTopics = topicRepository.selectAll()

        val topics = consumers.map { consumer ->
            val (id, topic, initialRunningState) = consumer.consumerConfig

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

    private fun startConsumerClients() {
        val topics = getTopics().filter { it.running }.map { it.topic }
        val clients = consumerClients.filterKeys { it in topics }.values
        clients.forEach { it.start() }
    }

    private fun getUpdatedTopicsOnly(updated: List<Topic>, current: List<Topic>) =
        updated.filter { x -> current.any { y -> y.id == x.id && y.running != x.running } }

    private fun configureConsumersTopics(repository: KafkaConsumerRepository): List<KafkaConsumerClientBuilder.TopicConfig<String, JsonElement>> {
        return group.consumers.map { consumer ->
            KafkaConsumerClientBuilder.TopicConfig<String, JsonElement>()
                .withStoreOnFailure(repository)
                .withLogging()
                .withConsumerConfig(
                    consumer.consumerConfig.topic,
                    stringDeserializer(),
                    ArenaJsonElementDeserializer(),
                    Consumer { event ->
                        runBlocking {
                            consumer.processEvent(event.value())
                        }
                    }
                )
        }
    }
}
