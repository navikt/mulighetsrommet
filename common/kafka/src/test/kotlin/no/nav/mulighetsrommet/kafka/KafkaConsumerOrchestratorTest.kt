package no.nav.mulighetsrommet.kafka

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.extensions.install
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.testcontainers.kafka.KafkaContainerExtension
import io.kotest.extensions.testcontainers.kafka.stringStringProducer
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
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
    val kafka = install(KafkaContainerExtension(DockerImageName.parse("confluentinc/cp-kafka:6.2.1"))) {
        withEmbeddedZookeeper()
    }

    val database = extension(FlywayDatabaseTestListener(testDatabaseConfig))

    fun KafkaContainer.getConsumerProperties(groupId: String = "consumer") = KafkaPropertiesBuilder.consumerBuilder()
        .withBrokerUrl(bootstrapServers)
        .withBaseProperties()
        .withConsumerGroupId(groupId)
        .withDeserializers(ByteArrayDeserializer::class.java, ByteArrayDeserializer::class.java)
        .build()

    fun uniqueTopicName() = UUID.randomUUID().toString()

    beforeSpec {
        kafka.start()
    }

    afterSpec {
        kafka.close()
    }

    afterEach {
        database.truncateAll()
    }

    val defaultConfig = KafkaConsumerOrchestrator.Config(
        consumerInitialRunningState = true,
        consumerRunningStatePollDelay = Long.MAX_VALUE,
    )

    test("should store topics based on provided consumers during setup") {
        val config = KafkaTopicConsumer.Config(id = "1", topic = "foo", kafka.getConsumerProperties())
        val consumer = TestConsumer()

        val orchestrator = KafkaConsumerOrchestrator(
            defaultConfig,
            database.db,
            mapOf(
                config to consumer,
            ),
        )

        orchestrator.getTopics() shouldContainExactly listOf(
            Topic(
                id = "1",
                topic = "foo",
                type = TopicType.CONSUMER,
                running = true,
            ),
        )
    }

    test("should update the consumer running state based on the topic configuration") {
        val config = KafkaTopicConsumer.Config(id = "1", topic = "foo", kafka.getConsumerProperties())
        val consumer = TestConsumer()

        val orchestrator = KafkaConsumerOrchestrator(
            KafkaConsumerOrchestrator.Config(consumerInitialRunningState = true, consumerRunningStatePollDelay = 10),
            database.db,
            mapOf(
                config to consumer,
            ),
        )

        orchestrator.getConsumers().first().isRunning shouldBe true

        orchestrator.updateRunningTopics(
            orchestrator.getTopics().map { it.copy(running = false) },
        )

        eventually(3.seconds) {
            orchestrator.getConsumers().first().isRunning shouldBe false
        }
    }

    test("consumer should process events from topic") {
        val topic = uniqueTopicName()

        val producer = kafka.stringStringProducer()
        producer.send(ProducerRecord(topic, null, "true"))
        producer.send(ProducerRecord(topic, "key1", "true"))
        producer.send(ProducerRecord(topic, "key2", null))
        producer.close()

        val config = KafkaTopicConsumer.Config(id = "1", topic = topic, kafka.getConsumerProperties())
        val consumer = spyk(TestConsumer())

        KafkaConsumerOrchestrator(
            defaultConfig,
            database.db,
            mapOf(
                config to consumer,
            ),
        )

        eventually(5.seconds) {
            coVerify(exactly = 1) {
                consumer.consume(null, "true")
                consumer.consume("key1", "true")
                consumer.consume("key2", null)
            }
        }
    }

    test("multiple consumers should be able to process events from the same topic") {
        val topic = uniqueTopicName()

        val producer = kafka.stringStringProducer()
        producer.send(ProducerRecord(topic, "key1", "true"))
        producer.close()

        val config1 = KafkaTopicConsumer.Config(id = "1", topic = topic, kafka.getConsumerProperties("group-1"))
        val consumer1 = spyk(TestConsumer())

        val config2 = KafkaTopicConsumer.Config(id = "2", topic = topic, kafka.getConsumerProperties("group-2"))
        val consumer2 = spyk(TestConsumer())

        KafkaConsumerOrchestrator(
            defaultConfig,
            database.db,
            mapOf(
                config1 to consumer1,
                config2 to consumer2,
            ),
        )

        eventually(5.seconds) {
            coVerify(exactly = 1) {
                consumer1.consume("key1", "true")
                consumer2.consume("key1", "true")
            }
        }
    }

    test("consumer should process json events from topic") {
        val topic = uniqueTopicName()

        val producer = kafka.stringStringProducer()
        producer.send(ProducerRecord(topic, "key1", """{ "success": true }"""))
        producer.send(ProducerRecord(topic, "key2", null))
        producer.close()

        val config = KafkaTopicConsumer.Config(id = "2", topic, kafka.getConsumerProperties())
        val consumer = spyk(JsonTestConsumer())

        KafkaConsumerOrchestrator(
            defaultConfig,
            database.db,
            mapOf(
                config to consumer,
            ),
        )
        eventually(5.seconds) {
            coVerify(exactly = 1) {
                consumer.consume("key1", Json.parseToJsonElement("""{ "success": true }"""))
                consumer.consume("key2", JsonNull)
            }
        }
    }

    test("failed events should be handled gracefully and kept in the topic consumer repository") {
        val topic = uniqueTopicName()

        val producer = kafka.stringStringProducer()
        producer.send(ProducerRecord(topic, "false"))
        producer.close()

        val config = KafkaTopicConsumer.Config(id = "1", topic, kafka.getConsumerProperties())
        val consumer = spyk(TestConsumer())

        val orchestrator = KafkaConsumerOrchestrator(
            defaultConfig,
            database.db,
            mapOf(
                config to consumer,
            ),
        )

        eventually(5.seconds) {
            coVerify(exactly = 1) {
                consumer.consume(null, "false")
            }
            orchestrator.getAllStoredConsumerRecords().shouldHaveSize(1).first().should {
                it.topic shouldBe topic
            }
        }
    }
})
