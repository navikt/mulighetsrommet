package no.nav.mulighetsrommet.kafka

import com.sksamuel.hoplite.ConfigLoader
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.http.*
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

    val client = HttpClient(CIO) {
        defaultRequest {
            url.takeFrom(URLBuilder().takeFrom(config.endpoints.get("mulighetsrommetBackend")!!).apply {
                encodedPath += url.encodedPath
            })
        }
    }

    initializeServer(config, Kafka(config.kafka, preset, Database(config.database), client))
}
