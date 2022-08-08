package no.nav.mulighetsrommet.arena.adapter.kafka

import io.kotest.common.runBlocking
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.delay
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.common.kafka.producer.util.KafkaProducerClientBuilder
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import no.nav.mulighetsrommet.arena.adapter.repositories.Topic
import no.nav.mulighetsrommet.arena.adapter.repositories.TopicRepository
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.Password
import no.nav.mulighetsrommet.database.kotest.extensions.DatabaseListener
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.admin.KafkaAdminClient
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.util.*

fun createDatabaseConfigWithRandomSchema(
    host: String = "localhost",
    port: Int = 5443,
    name: String = "mulighetsrommet-arena-adapter-db",
    user: String = "valp",
    password: Password = Password("valp")
): DatabaseConfig {
    val schema = "${UUID.randomUUID()}"
    return DatabaseConfig(host, port, name, schema, user, password, 1)
}

internal class KafkaConsumerOrchestratorTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val topicName = "tiltakendret"

    lateinit var producer: KafkaProducerClient<String, String>
    lateinit var consumerProperties: Properties
    val listener =
        DatabaseListener(createDatabaseConfigWithRandomSchema())
    register(listener)

    val kafkaContainer =
        KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:5.4.3"))

    beforeSpec {
        kafkaContainer.start()

        val brokerUrl = kafkaContainer.bootstrapServers

        val producerProperties = KafkaPropertiesBuilder.producerBuilder()
            .withBrokerUrl(brokerUrl)
            .withBaseProperties()
            .withProducerId("producer")
            .withSerializers(
                StringSerializer::class.java,
                StringSerializer::class.java
            )
            .build()

        producer =
            KafkaProducerClientBuilder.builder<String, String>()
                .withProperties(producerProperties)
                .build()

        consumerProperties = KafkaPropertiesBuilder.consumerBuilder()
            .withBrokerUrl(brokerUrl)
            .withBaseProperties()
            .withConsumerGroupId("consumer")
            .withDeserializers(
                ByteArrayDeserializer::class.java,
                ByteArrayDeserializer::class.java
            )
            .build()

        val admin =
            KafkaAdminClient.create(
                mapOf(
                    CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG to
                        brokerUrl
                )
            )

        admin.deleteTopics(
            listOf(
                topicName
            )
        )

        admin.createTopics(
            listOf(
                NewTopic(topicName, 1, 1)
            )
        )

        admin.close()
    }

    test("consumer starts processing event from producer") {
        val topicRepository: TopicRepository =
            mockk(relaxed = true)
        every { topicRepository.selectAll() } answers {
            listOf(
                Topic(1, "key", topicName, mockk(), true)
            )
        }
        val consumer: TopicConsumer<Any> = mockk()
        every { consumer.consumerConfig.topic } answers { topicName }

        val kafka = KafkaConsumerOrchestrator(
            consumerProperties,
            listener.db,
            ConsumerGroup(listOf(consumer)),
            topicRepository,
            200
        )

        producer.send(
            ProducerRecord(
                topicName,
                "key",
                "value"
            )
        )
        producer.close()

        runBlocking { delay(3000) }

        verify(exactly = 1) {
            runBlocking {
                consumer.processEvent(any())
            }
        }
    }

    afterSpec {
        kafkaContainer.stop()
    }
})
