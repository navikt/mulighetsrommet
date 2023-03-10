package no.nav.mulighetsrommet.kafka

import io.kotest.assertions.timing.eventually
import io.kotest.core.extensions.install
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.extensions.testcontainers.TestContainerExtension
import io.kotest.extensions.testcontainers.kafka.createStringStringProducer
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.spyk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.util.*
import kotlin.time.Duration.Companion.seconds

class KafkaConsumerOrchestratorTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val kafka = install(
        TestContainerExtension(
            KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.2.1"))
        )
    ) { withEmbeddedZookeeper() }

    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    fun KafkaContainer.getConsumerProperties() = KafkaPropertiesBuilder.consumerBuilder()
        .withBrokerUrl(bootstrapServers)
        .withBaseProperties()
        .withConsumerGroupId("consumer")
        .withDeserializers(ByteArrayDeserializer::class.java, ByteArrayDeserializer::class.java)
        .build()

    fun uniqueTopicName() = UUID.randomUUID().toString()

    beforeSpec {
        kafka.start()
    }

    afterSpec {
        kafka.close()
    }

    beforeEach {
        database.db.migrate()
    }

    afterEach {
        database.db.clean()
    }

    test("should store topics based on provided consumers during setup") {
        val consumer = TestConsumer("foo")

        val orchestrator = KafkaConsumerOrchestrator(
            KafkaConsumerOrchestrator.Config(topicStatePollDelay = Long.MAX_VALUE),
            kafka.getConsumerProperties(),
            database.db,
            listOf(consumer),
        )

        orchestrator.getTopics() shouldContainExactly listOf(
            Topic(
                id = "foo",
                topic = "foo",
                type = TopicType.CONSUMER,
                running = true
            )
        )
    }

    test("should update the consumer running state based on the topic configuration") {
        val consumer = TestConsumer("foo")

        val orchestrator = KafkaConsumerOrchestrator(
            KafkaConsumerOrchestrator.Config(topicStatePollDelay = 10),
            kafka.getConsumerProperties(),
            database.db,
            listOf(consumer),
        )

        orchestrator.getConsumers().first().isRunning shouldBe true

        orchestrator.updateRunningTopics(
            orchestrator.getTopics().map { it.copy(running = false) }
        )

        eventually(3.seconds) {
            orchestrator.getConsumers().first().isRunning shouldBe false
        }
    }

    test("consumer should process events from topic") {
        val topic = uniqueTopicName()

        val producer = kafka.createStringStringProducer()
        producer.send(ProducerRecord(topic, null, "true"))
        producer.send(ProducerRecord(topic, "key1", "true"))
        producer.send(ProducerRecord(topic, "key2", null))
        producer.close()

        val consumer = spyk(TestConsumer(topic))

        KafkaConsumerOrchestrator(
            KafkaConsumerOrchestrator.Config(topicStatePollDelay = Long.MAX_VALUE),
            kafka.getConsumerProperties(),
            database.db,
            listOf(consumer),
        )

        eventually(5.seconds) {
            coVerify(exactly = 1) {
                consumer.consume(null, "true")
                consumer.consume("key1", "true")
                consumer.consume("key2", null)
            }
        }
    }

    test("consumer should process json events from topic") {
        val topic = uniqueTopicName()

        val producer = kafka.createStringStringProducer()
        producer.send(ProducerRecord(topic, "key1", """{ "success": true }"""))
        producer.send(ProducerRecord(topic, "key2", null))
        producer.close()

        val consumer = spyk(JsonTestConsumer(topic))

        KafkaConsumerOrchestrator(
            KafkaConsumerOrchestrator.Config(topicStatePollDelay = Long.MAX_VALUE),
            kafka.getConsumerProperties(),
            database.db,
            listOf(consumer),
        )
        eventually(5.seconds) {
            coVerify(exactly = 1) {
                consumer.consume("key1", Json.parseToJsonElement("""{ "success": true }"""))
                consumer.consume("key2", JsonNull)
            }
        }
    }


    test("failed events should be handled gracefully and kept in the topic consumer repository") {
        val consumerRepository = KafkaConsumerRepository(database.db)
        val topic = uniqueTopicName()

        val producer = kafka.createStringStringProducer()
        producer.send(ProducerRecord(topic, "false"))
        producer.close()

        val consumer = spyk(TestConsumer(topic))

        KafkaConsumerOrchestrator(
            KafkaConsumerOrchestrator.Config(topicStatePollDelay = Long.MAX_VALUE),
            kafka.getConsumerProperties(),
            database.db,
            listOf(consumer),
        )

        eventually(5.seconds) {
            coVerify(exactly = 1) {
                consumer.consume(null, "false")
            }

            consumerRepository.getRecords(topic, 0, 1) shouldHaveSize 1
        }
    }
})
