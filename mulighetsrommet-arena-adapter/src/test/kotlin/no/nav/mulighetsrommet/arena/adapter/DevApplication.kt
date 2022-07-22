package no.nav.mulighetsrommet.arena.adapter

import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import com.sksamuel.hoplite.ConfigLoader
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder
import no.nav.mulighetsrommet.arena.adapter.consumers.*
import no.nav.mulighetsrommet.arena.adapter.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.arena.adapter.services.TopicService
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

fun main() {
    val (server, app) = ConfigLoader().loadConfigOrThrow<Config>("/application.yaml")

    // Referer her til mock-oauth2-server som er hostet gjennom lokal docker-compose.
    // Her er det også satt opp config som gjør at mock-oauth2-server returnerer et gyldig token når vi ber om et
    // machine to machine token for scopet "default"
    val tokenClient = AzureAdTokenClientBuilder.builder()
        .withClientId("mulighetsrommet-arena-adapter")
        .withPrivateJwk(createRSAKey("azure").toJSONString())
        .withTokenEndpointUrl("http://localhost:8081/azure/token")
        .buildMachineToMachineTokenClient()

    val api = MulighetsrommetApiClient(app.services.mulighetsrommetApi.url) {
        tokenClient.createMachineToMachineToken(app.services.mulighetsrommetApi.scope)
    }

    val preset = KafkaPropertiesBuilder.consumerBuilder()
        .withBrokerUrl(app.kafka.brokers)
        .withBaseProperties()
        .withConsumerGroupId(app.kafka.consumerGroupId)
        .withDeserializers(ByteArrayDeserializer::class.java, ByteArrayDeserializer::class.java)
        .build()

    val db = Database(app.database)
    val topicService = TopicService(db)

    val consumers = listOf(
        TiltakEndretConsumer(db, "tiltakendret", app.kafka.getTopic("tiltakendret"), api),
        TiltakgjennomforingEndretConsumer(db, "tiltakgjennomforingendret", app.kafka.getTopic("tiltakgjennomforingendret"), api),
        TiltakdeltakerEndretConsumer(db, "tiltakdeltakerendret", app.kafka.getTopic("tiltakdeltakerendret"), api),
        SakEndretConsumer(db, "sakendret", app.kafka.getTopic("sakendret"), api)
    )

    topicService.upsertConsumerTopics(consumers)

    val kafka = KafkaConsumerOrchestrator(preset, db, consumers, topicService)

    initializeServer(server) {
        configure(app, kafka, db, topicService)
    }
}

fun createRSAKey(keyID: String): RSAKey = KeyPairGenerator
    .getInstance("RSA").let {
        it.initialize(2048)
        it.generateKeyPair()
    }.let {
        RSAKey.Builder(it.public as RSAPublicKey)
            .privateKey(it.private as RSAPrivateKey)
            .keyUse(KeyUse.SIGNATURE)
            .keyID(keyID)
            .build()
    }
