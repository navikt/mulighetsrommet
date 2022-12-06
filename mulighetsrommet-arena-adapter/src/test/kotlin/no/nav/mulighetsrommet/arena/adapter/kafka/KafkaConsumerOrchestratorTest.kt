package no.nav.mulighetsrommet.arena.adapter.kafka

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
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import no.nav.mulighetsrommet.arena.adapter.repositories.Topic
import no.nav.mulighetsrommet.arena.adapter.repositories.TopicRepository
import no.nav.mulighetsrommet.arena.adapter.repositories.TopicType
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseListener
import no.nav.mulighetsrommet.database.kotest.extensions.createArenaAdapterDatabaseTestSchema
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.util.*
import kotlin.time.Duration.Companion.seconds

class KafkaConsumerOrchestratorTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val database = extension(FlywayDatabaseListener(createArenaAdapterDatabaseTestSchema()))

    val kafka = install(
        TestContainerExtension(
            KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.2.1"))
        )
    ) { withEmbeddedZookeeper() }

    fun KafkaContainer.getConsumerProperties() = KafkaPropertiesBuilder.consumerBuilder()
        .withBrokerUrl(bootstrapServers)
        .withBaseProperties()
        .withConsumerGroupId("consumer")
        .withDeserializers(
            ByteArrayDeserializer::class.java,
            ByteArrayDeserializer::class.java
        )
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
            kafka.getConsumerProperties(),
            database.db,
            ConsumerGroup(listOf(consumer)),
            TopicRepository(database.db),
            Long.MAX_VALUE
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
            kafka.getConsumerProperties(),
            database.db,
            ConsumerGroup(listOf(consumer)),
            TopicRepository(database.db),
            300
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
        producer.send(ProducerRecord(topic, """{ "success": true }"""))
        producer.close()

        val consumer = spyk(TestConsumer(topic))

        KafkaConsumerOrchestrator(
            kafka.getConsumerProperties(),
            database.db,
            ConsumerGroup(listOf(consumer)),
            TopicRepository(database.db),
            Long.MAX_VALUE
        )

        eventually(5.seconds) {
            coVerify {
                consumer.run(Json.parseToJsonElement("""{ "success": true }"""))
            }
        }
    }

    test("failed events should be handled gracefully and kept in the topic consumer repository") {
        val consumerRepository = KafkaConsumerRepository(database.db)
        val topic = uniqueTopicName()

        val producer = kafka.createStringStringProducer()
        producer.send(ProducerRecord(topic, """{ "success": false }"""))
        producer.close()

        val consumer = spyk(TestConsumer(topic))

        KafkaConsumerOrchestrator(
            kafka.getConsumerProperties(),
            database.db,
            ConsumerGroup(listOf(consumer)),
            TopicRepository(database.db),
            Long.MAX_VALUE
        )

        eventually(5.seconds) {
            coVerify {
                consumer.run(Json.parseToJsonElement("""{ "success": false }"""))
            }

            consumerRepository.getRecords(topic, 0, 1) shouldHaveSize 1
        }
    }
})
