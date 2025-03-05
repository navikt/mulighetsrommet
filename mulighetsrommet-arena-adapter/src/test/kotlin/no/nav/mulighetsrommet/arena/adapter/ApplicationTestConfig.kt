package no.nav.mulighetsrommet.arena.adapter

import io.ktor.server.testing.*
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEventService
import no.nav.mulighetsrommet.arena.adapter.tasks.NotifyFailedEvents
import no.nav.mulighetsrommet.arena.adapter.tasks.RetryFailedEvents
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.database.kotest.extensions.createRandomDatabaseConfig
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.tokenprovider.createMockRSAKey
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.apache.kafka.common.serialization.ByteArrayDeserializer

val databaseConfig: DatabaseConfig = createRandomDatabaseConfig("mr-arena-adapter")

fun <R> withTestApplication(
    oauth: MockOAuth2Server = MockOAuth2Server(),
    config: AppConfig = createTestApplicationConfig(oauth),
    test: suspend ApplicationTestBuilder.() -> R,
) {
    testApplication {
        application {
            configure(config)
        }

        test()
    }
}

fun createTestApplicationConfig(oauth: MockOAuth2Server) = AppConfig(
    database = databaseConfig,
    flyway = FlywayMigrationManager.MigrationConfig(),
    auth = createAuthConfig(oauth),
    kafka = createKafkaConfig(),
    enableFailedRecordProcessor = false,
    tasks = TaskConfig(
        retryFailedEvents = RetryFailedEvents.Config(
            delayOfMinutes = 1,
        ),
        notifyFailedEvents = NotifyFailedEvents.Config(
            maxRetries = 5,
            cron = "0 0 0 1 1 0",
        ),
    ),
    services = ServiceConfig(
        mulighetsrommetApi = ServiceClientConfig(url = "mulighetsrommet-api", scope = ""),
        tiltakshistorikk = ServiceClientConfig(url = "tiltakshistorikk", scope = ""),
        arenaEventService = ArenaEventService.Config(
            channelCapacity = 0,
            numChannelConsumers = 0,
            maxRetries = 0,
        ),
        arenaOrdsProxy = ServiceClientConfig(url = "arena-ords-proxy", scope = ""),
    ),
    slack = SlackConfig(
        token = "",
        channel = "",
        enable = false,
    ),
)

fun createKafkaConfig(): KafkaConfig {
    return run {
        val brokerUrl = "localhost:29092"
        val consumerGroupId = "mulighetsrommet-kafka-consumer.v1"
        KafkaConfig(
            consumers = KafkaConsumers(
                KafkaTopicConsumer.Config("tiltakendret", "tiltakendret"),
                KafkaTopicConsumer.Config("tiltakgjennomforingendret", "tiltakgjennomforingendret"),
                KafkaTopicConsumer.Config("tiltakdeltakerendret", "tiltakdeltakerendret"),
                KafkaTopicConsumer.Config("hist-tiltakdeltakerendret", "hist-tiltakdeltakerendret"),
                KafkaTopicConsumer.Config("sakendret", "sakendret"),
                KafkaTopicConsumer.Config("avtaleinfoendret", "avtaleinfoendret"),
            ),
            consumerPreset = KafkaPropertiesBuilder.consumerBuilder()
                .withBaseProperties()
                .withConsumerGroupId(consumerGroupId)
                .withBrokerUrl(brokerUrl)
                .withDeserializers(ByteArrayDeserializer::class.java, ByteArrayDeserializer::class.java)
                .build(),
        )
    }
}

// Default values for 'iss' og 'aud' in tokens issued by mock-oauth2-server is 'default'.
// These values are set as the default here so that standard tokens issued by MockOAuth2Server works with a minimal amount of setup.
fun createAuthConfig(
    oauth: MockOAuth2Server,
    issuer: String = "default",
    audience: String = "default",
    privateJwk: String = createMockRSAKey("azure"),
): AuthConfig {
    return AuthConfig(
        azure = AuthProvider(
            issuer = oauth.issuerUrl(issuer).toString(),
            jwksUri = oauth.jwksUrl(issuer).toUri().toString(),
            audience = audience,
            tokenEndpointUrl = oauth.tokenEndpointUrl(issuer).toString(),
            privateJwk = privateJwk,
        ),
    )
}
