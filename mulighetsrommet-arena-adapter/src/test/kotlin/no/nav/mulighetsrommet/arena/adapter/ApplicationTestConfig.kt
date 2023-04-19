package no.nav.mulighetsrommet.arena.adapter

import io.ktor.server.testing.*
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEventService
import no.nav.mulighetsrommet.arena.adapter.tasks.NotifyFailedEvents
import no.nav.mulighetsrommet.arena.adapter.tasks.RetryFailedEvents
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.FlywayDatabaseAdapter
import no.nav.mulighetsrommet.database.kotest.extensions.createDatabaseTestSchema
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.koin.ktor.ext.inject

fun createDatabaseTestConfig() = createDatabaseTestSchema("mulighetsrommet-arena-adapter-db", 5443)

fun <R> withTestApplication(
    oauth: MockOAuth2Server = MockOAuth2Server(),
    config: AppConfig = createTestApplicationConfig(oauth),
    test: suspend ApplicationTestBuilder.() -> R,
) {
    var flywayAdapter: FlywayDatabaseAdapter? = null

    testApplication {
        application {
            configure(config)

            val db by inject<Database>()
            flywayAdapter = db as FlywayDatabaseAdapter
        }

        test()
    }

    // Småhacky måte å rydde opp databasen etter at testen er ferdig
    flywayAdapter?.clean()
}

fun createTestApplicationConfig(oauth: MockOAuth2Server) = AppConfig(
    database = createDatabaseTestConfig(),
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
    return KafkaConfig(
        brokerUrl = "localhost:29092",
        consumerGroupId = "mulighetsrommet-kafka-consumer.v1",
        consumers = KafkaConsumers(
            KafkaTopicConsumer.Config("tiltakendret", "tiltakendret"),
            KafkaTopicConsumer.Config("tiltakgjennomforingendret", "tiltakgjennomforingendret"),
            KafkaTopicConsumer.Config("tiltakdeltakerendret", "tiltakdeltakerendret"),
            KafkaTopicConsumer.Config("sakendret", "sakendret"),
            KafkaTopicConsumer.Config("avtaleinfoendret", "avtaleinfoendret"),
        ),
    )
}

// Default values for 'iss' og 'aud' in tokens issued by mock-oauth2-server is 'default'.
// These values are set as the default here so that standard tokens issued by MockOAuth2Server works with a minimal amount of setup.
fun createAuthConfig(
    oauth: MockOAuth2Server,
    issuer: String = "default",
    audience: String = "default",
): AuthConfig {
    return AuthConfig(
        azure = AuthProvider(
            issuer = oauth.issuerUrl(issuer).toString(),
            jwksUri = oauth.jwksUrl(issuer).toUri().toString(),
            audience = audience,
            tokenEndpointUrl = oauth.tokenEndpointUrl(issuer).toString(),
        ),
    )
}
