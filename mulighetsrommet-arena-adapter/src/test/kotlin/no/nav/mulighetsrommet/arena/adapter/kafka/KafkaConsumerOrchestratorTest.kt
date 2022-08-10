package no.nav.mulighetsrommet.arena.adapter.kafka

import io.kotest.common.runBlocking
import io.kotest.core.extensions.install
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.extensions.testcontainers.TestContainerExtension
import io.kotest.extensions.testcontainers.kafka.createStringStringProducer
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.delay
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import no.nav.mulighetsrommet.arena.adapter.no.nav.mulighetsrommet.arena.adapter.utils.createDatabaseConfigWithRandomSchema
import no.nav.mulighetsrommet.arena.adapter.repositories.Topic
import no.nav.mulighetsrommet.arena.adapter.repositories.TopicRepository
import no.nav.mulighetsrommet.database.kotest.extensions.DatabaseListener
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.util.*

internal class KafkaConsumerOrchestratorTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    lateinit var consumerProperties: Properties
    val listener =
        DatabaseListener(createDatabaseConfigWithRandomSchema())
    register(listener)

    val kafka = install(
        TestContainerExtension(
            KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.2.1"))
        )
    ) {
        withEmbeddedZookeeper()
    }

    val producer = kafka.createStringStringProducer()

    val topicName1 = "topic1"
    val topicName2 = "topic2"
    val topicName3 = "topic3"

    val key1 = "key1"
    val key2 = "key2"
    val key3 = "key3"

    val value1 = "value1"
    val value2 = "value2"

    val topicRepository: TopicRepository = mockk(relaxed = true)

    val consumer1: TopicConsumer<Any> = mockk()
    val consumer2: TopicConsumer<Any> = mockk()
    val consumer3: TopicConsumer<Any> = mockk()

    beforeSpec {

        val brokerUrl = kafka.bootstrapServers

        consumerProperties =
            KafkaPropertiesBuilder.consumerBuilder()
                .withBrokerUrl(brokerUrl)
                .withBaseProperties()
                .withConsumerGroupId("consumer")
                .withDeserializers(
                    ByteArrayDeserializer::class.java,
                    ByteArrayDeserializer::class.java
                )
                .build()
    }

    test("consumers processes event from correct topic and inserts event into failed events on fail") {
        every { topicRepository.selectAll() } answers {
            listOf(
                Topic(1, key1, topicName1, mockk(), true),
                Topic(2, key2, topicName2, mockk(), true),
                Topic(3, key3, topicName3, mockk(), true)
            )
        }

        every { consumer1.consumerConfig.topic } answers { topicName1 }
        every { runBlocking { consumer1.processEvent(any()) } } returns Unit

        every { consumer2.consumerConfig.topic } answers { topicName2 }
        every { runBlocking { consumer2.processEvent(any()) } } throws Exception()

        every { consumer3.consumerConfig.topic } answers { topicName3 }

        val orchestrator = KafkaConsumerOrchestrator(
            consumerProperties,
            listener.db,
            ConsumerGroup(
                listOf(consumer1, consumer2, consumer3)
            ),
            topicRepository,
            200
        )

        producer.send(
            ProducerRecord(
                topicName2,
                key2,
                value2
            )
        )
        producer.send(
            ProducerRecord(
                topicName1,
                key1,
                value1
            )
        )
        producer.close()

        runBlocking { delay(1000) }

        verify(exactly = 1) {
            runBlocking {
                consumer1.processEvent(
                    JsonElementDeserializer().deserialize(
                        topicName1,
                        value1.toByteArray()
                    )
                )
            }
        }

        verify(exactly = 1) {
            runBlocking {
                consumer2.processEvent(
                    JsonElementDeserializer().deserialize(
                        topicName2,
                        value2.toByteArray()
                    )
                )
            }
        }

        verify(exactly = 0) {
            runBlocking {
                consumer3.processEvent(any())
            }
        }

        val consumerRepository =
            KafkaConsumerRepository(listener.db)

        consumerRepository.hasRecordWithKey(
            topicName1,
            0,
            key1.toByteArray()
        ) shouldBe false

        consumerRepository.hasRecordWithKey(
            topicName2,
            0,
            key2.toByteArray()
        ) shouldBe true
    }
})
