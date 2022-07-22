package no.nav.mulighetsrommet.arena.adapter.kafka

import com.sksamuel.hoplite.Masked
import io.kotest.common.runBlocking
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.delay
import no.nav.common.kafka.producer.util.KafkaProducerClientBuilder
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import no.nav.mulighetsrommet.arena.adapter.DatabaseConfig
import no.nav.mulighetsrommet.test.extensions.DatabaseListener
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringSerializer

fun createDatabaseConfigWithRandomSchema(
    host: String = "localhost",
    port: Int = 5443,
    name: String = "mulighetsrommet-arena-adapter-db",
    user: String = "valp",
    password: Masked = Masked("valp")
): DatabaseConfig {
    val schema = "${java.util.UUID.randomUUID()}"
    return DatabaseConfig(host, port, name, schema, user, password)
}

internal class KafkaConsumerOrchestratorTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val properties = KafkaPropertiesBuilder.producerBuilder()
        .withBrokerUrl("localhost:29092")
        .withBaseProperties()
        .withProducerId("producer")
        .withSerializers(
            StringSerializer::class.java,
            StringSerializer::class.java
        )
        .build()

    val producer =
        KafkaProducerClientBuilder.builder<String, String>()
            .withProperties(properties)
            .build()

    val preset = KafkaPropertiesBuilder.consumerBuilder()
        .withBrokerUrl("localhost:29092")
        .withBaseProperties()
        .withConsumerGroupId("consumer")
        .withDeserializers(
            ByteArrayDeserializer::class.java,
            ByteArrayDeserializer::class.java
        )
        .build()

    val listener =
        DatabaseListener(createDatabaseConfigWithRandomSchema())

    register(listener)

    beforeSpec {
    }

    test("consumer starts processing event from producer") {
        val consumer: TopicConsumer<*> = mockk()
        every { consumer.topic } answers { "tiltakendret" }
        val kafka = KafkaConsumerOrchestrator(
            preset,
            listener.db,
            listOf(consumer)
        )

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
})
