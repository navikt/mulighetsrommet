package no.nav.mulighetsrommet.arena.adapter.kafka

import com.sksamuel.hoplite.Masked
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
import no.nav.mulighetsrommet.arena.adapter.DatabaseConfig
import no.nav.mulighetsrommet.test.extensions.DatabaseListener
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
    password: Masked = Masked("valp")
): DatabaseConfig {
    val schema = "${UUID.randomUUID()}"
    return DatabaseConfig(host, port, name, schema, user, password)
}

internal class KafkaConsumerOrchestratorTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    lateinit var producer: KafkaProducerClient<String, String>
    lateinit var preset: Properties
    val listener =
        DatabaseListener(createDatabaseConfigWithRandomSchema())
    register(listener)

    val kafkaContainer =
        KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:5.4.3"))

    beforeSpec {
        kafkaContainer.start()

        val brokerUrl = kafkaContainer.bootstrapServers

        val properties = KafkaPropertiesBuilder.producerBuilder()
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
                .withProperties(properties)
                .build()

        preset = KafkaPropertiesBuilder.consumerBuilder()
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
                "tiltakendret"
            )
        )

        admin.createTopics(
            listOf(
                NewTopic("tiltakendret", 1, 1)
            )
        )

        admin.close()
    }

    test("consumer starts processing event from producer") {
        val consumer: TopicConsumer<*> = mockk()
        every { consumer.topic } answers { "tiltakendret" }
        println(listener.db)
        println("hei1")
        val kafka = KafkaConsumerOrchestrator(
            preset,
            listener.db,
            listOf(consumer)
        )
        println("hei2")

        kafka.enableTopicConsumption()

        producer.send(
            ProducerRecord(
                "tiltakendret",
                "key",
                "value"
            )
        )
        producer.close()

        runBlocking { delay(5000) }

        verify(exactly = 1) {
            consumer.processEvent(any())
        }
    }

    afterSpec {
        kafkaContainer.stop()
    }
})
