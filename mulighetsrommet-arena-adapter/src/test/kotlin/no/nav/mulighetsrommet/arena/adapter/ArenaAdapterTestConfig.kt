package no.nav.mulighetsrommet.arena.adapter.no.nav.mulighetsrommet.arena.adapter

import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import io.ktor.server.testing.*
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder
import no.nav.mulighetsrommet.arena.adapter.AppConfig
import no.nav.mulighetsrommet.arena.adapter.AuthConfig
import no.nav.mulighetsrommet.arena.adapter.AuthProvider
import no.nav.mulighetsrommet.arena.adapter.KafkaConfig
import no.nav.mulighetsrommet.arena.adapter.ServiceClientConfig
import no.nav.mulighetsrommet.arena.adapter.ServiceConfig
import no.nav.mulighetsrommet.arena.adapter.TaskConfig
import no.nav.mulighetsrommet.arena.adapter.TopicsConfig
import no.nav.mulighetsrommet.arena.adapter.configure
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEventService
import no.nav.mulighetsrommet.arena.adapter.tasks.RetryFailedEvents
import no.nav.mulighetsrommet.database.kotest.extensions.createArenaAdapterDatabaseTestSchema
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

fun <R> withArenaAdapterApp(
    oauth: MockOAuth2Server = MockOAuth2Server(),
    config: AppConfig = createTestApplicationConfig(oauth),
    test: suspend ApplicationTestBuilder.() -> R
) {
    val tokenClient = AzureAdTokenClientBuilder.builder()
        .withClientId(config.auth.azure.audience)
        .withPrivateJwk(createRSAKeyForLokalUtvikling("azure").toJSONString())
        .withTokenEndpointUrl(config.auth.azure.tokenEndpointUrl)
        .buildMachineToMachineTokenClient()

    val kafkaPreset = KafkaPropertiesBuilder.consumerBuilder()
        .withBrokerUrl(config.kafka.brokers)
        .withBaseProperties()
        .withConsumerGroupId(config.kafka.consumerGroupId)
        .withDeserializers(ByteArrayDeserializer::class.java, ByteArrayDeserializer::class.java)
        .build()

    testApplication {
        application {
            configure(config, kafkaPreset = kafkaPreset, tokenClient = tokenClient)
        }
        test()
    }
}

fun createTestApplicationConfig(oauth: MockOAuth2Server) = AppConfig(
    database = createArenaAdapterDatabaseTestSchema(),
    auth = createAuthConfig(oauth),
    kafka = createKafkaConfig(),
    enableFailedRecordProcessor = false,
    tasks = TaskConfig(
        retryFailedEvents = RetryFailedEvents.Config(
            delayOfMinutes = 1
        )
    ),
    services = ServiceConfig(
        mulighetsrommetApi = ServiceClientConfig(url = "", scope = ""),
        arenaEventService = ArenaEventService.Config(
            channelCapacity = 0,
            numChannelConsumers = 0,
            maxRetries = 0
        ),
        arenaOrdsProxy = ServiceClientConfig(url = "", scope = "")
    )
)

fun createKafkaConfig(): KafkaConfig {
    return KafkaConfig(
        "localhost:29092",
        "mulighetsrommet-kafka-consumer.v1",
        TopicsConfig(
            pollChangesDelayMs = 10000,
            consumer = mapOf(
                "tiltakendret" to "tiltakendret",
                "tiltakgjennomforingendret" to "tiltakgjennomforingendret",
                "tiltakdeltakerendret" to "tiltakdeltakerendret",
                "sakendret" to "sakendret"
            )
        )
    )
}

// Default values for 'iss' og 'aud' in tokens issued by mock-oauth2-server is 'default'.
// These values are set as the default here so that standard tokens issued by MockOAuth2Server works with a minimal amount of setup.
fun createAuthConfig(
    oauth: MockOAuth2Server,
    issuer: String = "default",
    audience: String = "default"
): AuthConfig {
    return AuthConfig(
        azure = AuthProvider(
            issuer = oauth.issuerUrl(issuer).toString(),
            jwksUri = oauth.jwksUrl(issuer).toUri().toString(),
            audience = audience,
            tokenEndpointUrl = oauth.tokenEndpointUrl(issuer).toString()
        )
    )
}

private fun createRSAKeyForLokalUtvikling(keyID: String): RSAKey = KeyPairGenerator
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
