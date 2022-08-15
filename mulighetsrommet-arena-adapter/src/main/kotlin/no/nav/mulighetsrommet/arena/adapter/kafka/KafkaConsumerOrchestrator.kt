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
        logger.debug("Initializing Kafka")

        updateTopics(group.consumers)

        val kafkaConsumerRepository = KafkaConsumerRepository(db)
        val consumerTopicsConfig = configureConsumersTopics(kafkaConsumerRepository)
        val lockProvider = JdbcLockProvider(db.dataSource)

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
        logger.debug("Started kafka consumer record processor")
    }

    fun disableFailedRecordProcessor() {
        consumerRecordProcessor.close()
        logger.debug("Stopped kafka processors")
    }

    fun getTopics() = topicRepository.selectAll()

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
                    logger.debug("Stopped ${it.topic}")
                } else if (!client.isRunning && it.running) {
                    client.start()
                    logger.debug("Started ${it.topic}")
                }
            }
        }
    }

    private fun updateTopics(consumers: List<TopicConsumer<*>>) = topicRepository.upsertTopics(consumers)

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
