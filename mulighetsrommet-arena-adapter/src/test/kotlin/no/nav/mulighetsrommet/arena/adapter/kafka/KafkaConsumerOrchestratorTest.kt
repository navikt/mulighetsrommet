package no.nav.mulighetsrommet.arena.adapter.kafka

import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.extensions.testcontainers.kafka.createStringStringProducer
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.mulighetsrommet.arena.adapter.no.nav.mulighetsrommet.arena.adapter.createKafkaTestContainerSetup
import no.nav.mulighetsrommet.arena.adapter.repositories.Topic
import no.nav.mulighetsrommet.arena.adapter.repositories.TopicRepository
import no.nav.mulighetsrommet.arena.adapter.repositories.TopicType
import org.apache.kafka.clients.producer.ProducerRecord
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class KafkaConsumerOrchestratorTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val (listener, kafka, consumerProperties) = createKafkaTestContainerSetup()

    val producer = kafka.createStringStringProducer()

    val keys = (0..2).map { "key$it" }
    val topicNames = (0..2).map { "topic$it" }
    val values = (0..1).map { "value$it" }

    val topicRepository: TopicRepository = mockk(relaxed = true)
    lateinit var consumerRepository: KafkaConsumerRepository

    val consumer1: TopicConsumer<Any> = mockk()
    val consumer2: TopicConsumer<Any> = mockk()
    val consumer3: TopicConsumer<Any> = mockk()

    beforeSpec {

        consumerRepository = KafkaConsumerRepository(listener.db)

        every { topicRepository.selectAll() } returns keys.mapIndexed { index, it -> Topic(index, it, topicNames[index], TopicType.CONSUMER, true) }

        every { consumer1.consumerConfig.topic } returns topicNames[0]
        coEvery { consumer1.processEvent(any()) } returns Unit

        every { consumer2.consumerConfig.topic } returns topicNames[1]
        coEvery { consumer2.processEvent(any()) } throws Exception()

        every { consumer3.consumerConfig.topic } returns topicNames[2]

        KafkaConsumerOrchestrator(
            consumerProperties,
            listener.db,
            ConsumerGroup(
                listOf(consumer1, consumer2, consumer3)
            ),
            topicRepository,
            Long.MAX_VALUE
        )

        values.forEachIndexed() { index, it -> producer.send(ProducerRecord(topicNames[index], keys[index], it)) }
        producer.close()
    }

    test("consumers processes event from correct topic and inserts event into failed events on fail") {
        eventually(Duration.Companion.seconds(3)) {

        coVerify(exactly = 1) {
            consumer1.processEvent(
                ArenaJsonElementDeserializer().deserialize(
                    topicNames[0],
                    values[0].toByteArray()
                )
            )
        }

        coVerify(exactly = 1) {
            consumer2.processEvent(
                ArenaJsonElementDeserializer().deserialize(
                    topicNames[1],
                    values[1].toByteArray()
                )
            )
        }

        coVerify(exactly = 0) {
            consumer3.processEvent(any())
        }


        consumerRepository.hasRecordWithKey(
            topicNames[0],
            0,
            keys[0].toByteArray()
        ) shouldBe false

        consumerRepository.hasRecordWithKey(
            topicNames[1],
            0,
            keys[1].toByteArray()
        ) shouldBe true

        }
    }
})
