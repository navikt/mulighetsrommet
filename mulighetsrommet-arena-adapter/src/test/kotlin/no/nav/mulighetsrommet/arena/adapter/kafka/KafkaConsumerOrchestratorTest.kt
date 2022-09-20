package no.nav.mulighetsrommet.arena.adapter.kafka

import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.extensions.testcontainers.kafka.createStringStringProducer
import io.kotest.matchers.shouldBe
import io.mockk.*
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

    val consumers = (0..2).map { mockk<TopicConsumer<Any>>() }

    beforeSpec {

        consumerRepository = KafkaConsumerRepository(listener.db)

        every { topicRepository.selectAll() } returns keys.mapIndexed { index, it ->
            Topic(
                index,
                it,
                topicNames[index],
                TopicType.CONSUMER,
                true
            )
        }

        topicNames.forEachIndexed { index, it -> every { consumers[index].consumerConfig.topic } returns it }

        coEvery { consumers[0].processEvent(any()) } returns Unit
        coEvery { consumers[1].processEvent(any()) } throws Exception()

        KafkaConsumerOrchestrator(
            consumerProperties,
            listener.db,
            ConsumerGroup(
                consumers
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
                consumers[0].processEvent(
                    ArenaJsonElementDeserializer().deserialize(
                        topicNames[0],
                        values[0].toByteArray()
                    )
                )
            }

            coVerify(exactly = 1) {
                consumers[1].processEvent(
                    ArenaJsonElementDeserializer().deserialize(
                        topicNames[1],
                        values[1].toByteArray()
                    )
                )
            }

            coVerify(exactly = 0) {
                consumers[2].processEvent(any())
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
