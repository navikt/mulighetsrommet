package no.nav.mulighetsrommet.arena.adapter.no.nav.mulighetsrommet.arena.adapter

import io.kotest.core.extensions.install
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.testcontainers.TestContainerExtension
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseListener
import no.nav.mulighetsrommet.database.kotest.extensions.createArenaAdapterDatabaseTestSchema
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.util.*

fun FunSpec.createKafkaTestContainerSetup(): Triple<FlywayDatabaseListener, KafkaContainer, Properties> {
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

    val consumerProperties =
        KafkaPropertiesBuilder.consumerBuilder()
            .withBrokerUrl(kafka.bootstrapServers)
            .withBaseProperties()
            .withConsumerGroupId("consumer")
            .withDeserializers(
                ByteArrayDeserializer::class.java,
                ByteArrayDeserializer::class.java
            )
            .build()

    return Triple(listener, kafka, consumerProperties)
}
