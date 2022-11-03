package no.nav.mulighetsrommet.arena.adapter.kafka

import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.extensions.testcontainers.kafka.createStringStringProducer
import io.kotest.matchers.collections.shouldHaveSize
import io.mockk.coVerify
import io.mockk.spyk
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.arena.adapter.no.nav.mulighetsrommet.arena.adapter.createKafkaTestContainerSetup
import no.nav.mulighetsrommet.arena.adapter.repositories.EventRepository
import no.nav.mulighetsrommet.arena.adapter.repositories.TopicRepository
import org.apache.kafka.clients.producer.ProducerRecord
import kotlin.time.Duration.Companion.seconds

class KafkaConsumerOrchestratorTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val (listener, kafka, consumerProperties) = createKafkaTestContainerSetup()

    val producer = kafka.createStringStringProducer()

    lateinit var topicRepository: TopicRepository
    lateinit var consumerRepository: KafkaConsumerRepository

    lateinit var consumer1: TestConsumer
    lateinit var consumer2: TestConsumer

    beforeSpec {
        consumer1 = spyk(TestConsumer("foo", EventRepository(listener.db)))
        consumer2 = spyk(TestConsumer("bar", EventRepository(listener.db)))

        topicRepository = TopicRepository(listener.db)
        consumerRepository = KafkaConsumerRepository(listener.db)

        val orchestrator = KafkaConsumerOrchestrator(
            consumerProperties,
            listener.db,
            ConsumerGroup(listOf(consumer1, consumer2)),
            topicRepository,
            Long.MAX_VALUE
        )

        producer.send(ProducerRecord("foo", """{ "success": true }"""))
        producer.send(ProducerRecord("bar", """{ "success": false }"""))
        producer.close()
    }

    test("consumer should process events from topic") {
        eventually(10.seconds) {
            coVerify { consumer1.processEvent(Json.parseToJsonElement("""{ "success": true }""")) }
        }
    }

    test("failed events should be handled gracefully and kept in the topic consumer repository") {
        eventually(10.seconds) {
            coVerify { consumer2.processEvent(Json.parseToJsonElement("""{ "success": false }""")) }

            consumerRepository.getRecords(
                "bar",
                0,
                1
            ) shouldHaveSize 1
        }
    }
})
