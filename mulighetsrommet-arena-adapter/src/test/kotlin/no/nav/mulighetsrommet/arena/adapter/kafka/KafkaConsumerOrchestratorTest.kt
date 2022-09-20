package no.nav.mulighetsrommet.arena.adapter.kafka

import io.kotest.core.extensions.install
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.extensions.testcontainers.TestContainerExtension
import io.kotest.extensions.testcontainers.kafka.createStringStringProducer
import io.ktor.client.engine.mock.*
import io.mockk.mockk
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import no.nav.mulighetsrommet.arena.adapter.ConsumerConfig
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.consumers.TiltakEndretConsumer
import no.nav.mulighetsrommet.arena.adapter.repositories.EventRepository
import no.nav.mulighetsrommet.arena.adapter.repositories.TopicRepository
import no.nav.mulighetsrommet.arena.adapter.utils.EventFaker.generateFakeEventDataSet
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseListener
import no.nav.mulighetsrommet.database.kotest.extensions.createArenaAdapterDatabaseTestSchema
import no.nav.mulighetsrommet.domain.arena.ArenaTiltak
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.assertj.db.api.Assertions
import org.assertj.db.type.Table
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.util.*

internal class KafkaConsumerOrchestratorTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    lateinit var consumerProperties: Properties
    val listener =
        FlywayDatabaseListener(createArenaAdapterDatabaseTestSchema())
    register(listener)

    val kafka = install(
        TestContainerExtension(
            KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.2.1"))
        )
    ) {
        withEmbeddedZookeeper()
    }

    val producer = kafka.createStringStringProducer()

//    val topicName1 = "topic1"
//    val topicName2 = "topic2"
//    val topicName3 = "topic3"
//
//    val key1 = "key1"
//    val key2 = "key2"
//    val key3 = "key3"
//
//    val value1 = "value1"
//    val value2 = "value2"
//
    val topicRepository: TopicRepository = mockk(relaxed = true)
//
//    val consumer1: TopicConsumer<Any> = mockk()
//    val consumer2: TopicConsumer<Any> = mockk()
//    val consumer3: TopicConsumer<Any> = mockk()

    beforeSpec {
        consumerProperties =
            KafkaPropertiesBuilder.consumerBuilder()
                .withBrokerUrl(kafka.bootstrapServers)
                .withBaseProperties()
                .withConsumerGroupId("consumer")
                .withDeserializers(
                    ByteArrayDeserializer::class.java,
                    ByteArrayDeserializer::class.java
                )
                .build()

//        every { topicRepository.selectAll() } answers {
//            listOf(
//                Topic(1, key1, topicName1, mockk(), true),
//                Topic(2, key2, topicName2, mockk(), true),
//                Topic(3, key3, topicName3, mockk(), true)
//            )
//        }
//
//        every { consumer1.consumerConfig.topic } answers { topicName1 }
//        every { runBlocking { consumer1.processEvent(any()) } } returns Unit
//
//        every { consumer2.consumerConfig.topic } answers { topicName2 }
//        every { runBlocking { consumer2.processEvent(any()) } } throws Exception()
//
//        every { consumer3.consumerConfig.topic } answers { topicName3 }
    }

//    test("consumers processes event from correct topic and inserts event into failed events on fail") {
//        val orchestrator = KafkaConsumerOrchestrator(
//            consumerProperties,
//            listener.db,
//            ConsumerGroup(
//                listOf(consumer1, consumer2, consumer3)
//            ),
//            topicRepository,
//            200
//        )
//
//        producer.send(
//            ProducerRecord(
//                topicName2,
//                key2,
//                value2
//            )
//        )
//        producer.send(
//            ProducerRecord(
//                topicName1,
//                key1,
//                value1
//            )
//        )
//        producer.close()
//
//        runBlocking { delay(1000) }
//
//        verify(exactly = 1) {
//            runBlocking {
//                consumer1.processEvent(
//                    ArenaJsonElementDeserializer().deserialize(
//                        topicName1,
//                        value1.toByteArray()
//                    )
//                )
//            }
//        }
//
//        verify(exactly = 1) {
//            runBlocking {
//                consumer2.processEvent(
//                    ArenaJsonElementDeserializer().deserialize(
//                        topicName2,
//                        value2.toByteArray()
//                    )
//                )
//            }
//        }
//
//        verify(exactly = 0) {
//            runBlocking {
//                consumer3.processEvent(any())
//            }
//        }
//
//        val consumerRepository =
//            KafkaConsumerRepository(listener.db)
//
//        consumerRepository.hasRecordWithKey(
//            topicName1,
//            0,
//            key1.toByteArray()
//        ) shouldBe false
//
//        consumerRepository.hasRecordWithKey(
//            topicName2,
//            0,
//            key2.toByteArray()
//        ) shouldBe true
//    }

    test("jepsi pepsi") {
        val (tiltakstyper) = generateFakeEventDataSet()
        tiltakstyper.map {
            val encoded = Json.encodeToString(it as ArenaTiltak)
           val jepsi = """
               {
                    "op_type": "I",
                    "before": $encoded,
                    "after": $encoded
               }
           """.trimIndent()
            println(jepsi)
            jepsi
        }.forEachIndexed {index, it -> producer.send(ProducerRecord("tiltakendret", index.toString(), it))}


        val eventRepository = EventRepository(listener.db)

        val engine = MockEngine { respondOk() }

        val client = MulighetsrommetApiClient(engine, maxRetries = 1, baseUri = "api") {
            "Bearer token"
        }

        val tiltakstyperConsumer = TiltakEndretConsumer(ConsumerConfig("tiltakendret", "tiltakendret"), eventRepository, client)

        val orchestrator = KafkaConsumerOrchestrator(
            consumerProperties,
            listener.db,
            ConsumerGroup(
                listOf(tiltakstyperConsumer)
            ),
            topicRepository,
            10000
        )

//        coVerify { eventRepository.saveEvent(any(), any(), any()) }

        val table = Table(listener.db.getDatasource(), "events")

        Assertions.assertThat(table).hasNumberOfRows(10)
    }

})
