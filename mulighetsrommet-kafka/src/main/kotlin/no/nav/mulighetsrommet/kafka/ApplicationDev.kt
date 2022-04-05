package no.nav.mulighetsrommet.kafka

import com.sksamuel.hoplite.ConfigLoader
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.http.*
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import org.apache.kafka.common.serialization.ByteArrayDeserializer

fun main() {
    val config = ConfigLoader().loadConfigOrThrow<Config>("/application.yaml")

    val preset = KafkaPropertiesBuilder.consumerBuilder()
        .withBrokerUrl(config.app.kafka.brokers)
        .withBaseProperties()
        .withConsumerGroupId(config.app.kafka.consumerGroupId)
        .withDeserializers(ByteArrayDeserializer::class.java, ByteArrayDeserializer::class.java)
        .build()

    val client = HttpClient(CIO) {
        defaultRequest {
            url.takeFrom(
                URLBuilder().takeFrom(config.app.endpoints.get("mulighetsrommetApi")!!).apply {
                    encodedPath += url.encodedPath
                }
            )
        }
    }

    initializeServer(config.app, Kafka(config.app.kafka, preset, Database(config.app.database), client))
}
