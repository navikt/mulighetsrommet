package no.nav.mulighetsrommet.arena.adapter

import com.sksamuel.hoplite.ConfigLoader
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import org.apache.kafka.common.serialization.ByteArrayDeserializer

fun main() {
    val (server, app) = ConfigLoader().loadConfigOrThrow<Config>("/application.yaml")

    val mulighetsrommetApiClient = MulighetsrommetApiClient(app.services.mulighetsrommetApi.url) {
        "TODO: mock-oauth2-server token"
    }

    val preset = KafkaPropertiesBuilder.consumerBuilder()
        .withBrokerUrl(app.kafka.brokers)
        .withBaseProperties()
        .withConsumerGroupId(app.kafka.consumerGroupId)
        .withDeserializers(ByteArrayDeserializer::class.java, ByteArrayDeserializer::class.java)
        .build()

    val db = Database(app.database)
    val kafka = Kafka(app.kafka, preset, db, mulighetsrommetApiClient)

    initializeServer(server) {
        main(kafka)
    }
}
