package no.nav.mulighetsrommet.kafka

import com.sksamuel.hoplite.ConfigLoader
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import org.apache.kafka.common.serialization.ByteArrayDeserializer

fun main(args: Array<String>) {
    val config = ConfigLoader().loadConfigOrThrow<AppConfig>("/application.yaml")

    val preset = KafkaPropertiesBuilder.consumerBuilder()
        .withBrokerUrl(config.kafka.brokers)
        .withBaseProperties()
        .withConsumerGroupId(config.kafka.consumerGroupId)
        .withDeserializers(ByteArrayDeserializer::class.java, ByteArrayDeserializer::class.java)
        .build()

    initializeServer(config, Kafka(config.kafka, preset, Database(config.database)))
}
