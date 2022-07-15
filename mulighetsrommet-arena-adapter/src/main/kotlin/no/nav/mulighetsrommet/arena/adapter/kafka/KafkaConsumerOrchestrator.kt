package no.nav.mulighetsrommet.arena.adapter

import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider
import no.nav.common.kafka.consumer.KafkaConsumerClient
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRecordProcessor
import no.nav.common.kafka.consumer.feilhandtering.util.KafkaConsumerRecordProcessorBuilder
import no.nav.common.kafka.consumer.util.ConsumerUtils.findConsumerConfigsWithStoreOnFailure
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.stringDeserializer
import no.nav.mulighetsrommet.arena.adapter.consumers.TopicConsumer
import no.nav.mulighetsrommet.arena.adapter.services.TopicService
import org.slf4j.LoggerFactory
import java.util.*
import java.util.function.Consumer

class KafkaConsumerOrchestrator(
    consumerPreset: Properties,
    db: Database,
    private val consumers: List<TopicConsumer<*>>,
    private val topicService: TopicService,
) {

    private val logger = LoggerFactory.getLogger(KafkaConsumerOrchestrator::class.java)
    private val consumerClients: Map<String, KafkaConsumerClient>
    private val consumerRecordProcessor: KafkaConsumerRecordProcessor

    init {
        logger.debug("Initializing Kafka")

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
    }

    private fun startConsumerClients() {
        val topics = topicService.getTopics().filter{it.running}.map{it.topic}
        val clients = consumerClients.filterKeys { it in topics }.values
        clients.forEach { it.start() }
    }

    fun enableFailedRecordProcessor() {
        consumerRecordProcessor.start()
        logger.debug("Started kafka consumer record processor")
    }

    fun disableFailedRecordProcessor() {
        consumerRecordProcessor.close()
        logger.debug("Stopped kafka processors")
    }

    fun setRunning(topic: String, value: Boolean) {
        val client = consumerClients[topic]
        if (client != null) {
        }
    }

    private fun configureConsumersTopics(repository: KafkaConsumerRepository): List<KafkaConsumerClientBuilder.TopicConfig<String, JsonElement>> {
        return consumers.map { consumer ->
            KafkaConsumerClientBuilder.TopicConfig<String, JsonElement>()
                .withStoreOnFailure(repository)
                .withLogging()
                .withConsumerConfig(
                    consumer.topic,
                    stringDeserializer(),
                    JsonElementDeserializer(),
                    Consumer { event ->
                        consumer.processEvent(event)
                    }
                )
        }
    }
}
