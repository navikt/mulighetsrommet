package no.nav.mulighetsrommet.kafka

import com.sksamuel.hoplite.ConfigLoader
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import org.apache.kafka.common.serialization.ByteArrayDeserializer

fun main() {
    val config = ConfigLoader().loadConfigOrThrow<Config>("/application.yaml")

    val mulighetsrommetApiClient = MulighetsrommetApiClient(config.app.endpoints.get("mulighetsrommetApi")!!)

    val preset = KafkaPropertiesBuilder.consumerBuilder()
        .withBrokerUrl(config.app.kafka.brokers)
        .withBaseProperties()
        .withConsumerGroupId(config.app.kafka.consumerGroupId)
        .withDeserializers(ByteArrayDeserializer::class.java, ByteArrayDeserializer::class.java)
        .build()

    initializeServer(config, Kafka(config.app.kafka, preset, Database(config.app.database), mulighetsrommetApiClient))
}




