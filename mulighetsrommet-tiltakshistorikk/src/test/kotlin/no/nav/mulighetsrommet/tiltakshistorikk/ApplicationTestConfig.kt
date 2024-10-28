package no.nav.mulighetsrommet.tiltakshistorikk

import io.ktor.client.engine.*
import io.ktor.server.testing.*
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.database.kotest.extensions.createDatabaseTestSchema
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.security.mock.oauth2.MockOAuth2Server

var databaseConfig: DatabaseConfig? = null
fun createDatabaseTestConfig() = if (databaseConfig == null) {
    databaseConfig = createDatabaseTestSchema("mr-tiltakshistorikk")
    databaseConfig!!
} else {
    databaseConfig!!
}

fun <R> withTestApplication(
    oauth: MockOAuth2Server = MockOAuth2Server(),
    httpClientEngine: HttpClientEngine = createMockEngine(),
    config: AppConfig = createTestApplicationConfig(oauth, httpClientEngine),
    test: suspend ApplicationTestBuilder.() -> R,
) {
    testApplication {
        application {
            configure(config)
        }

        test()
    }
}

fun createTestApplicationConfig(oauth: MockOAuth2Server, engine: HttpClientEngine) = AppConfig(
    httpClientEngine = engine,
    database = createDatabaseTestConfig(),
    flyway = FlywayMigrationManager.MigrationConfig(),
    auth = createAuthConfig(oauth),
    kafka = KafkaConfig(
        brokerUrl = "1",
        consumerGroupId = "1",
        consumers = KafkaConsumers(
            amtDeltakerV1 = KafkaTopicConsumer.Config(id = "amt-deltaker", topic = "amt-deltaker"),
            sisteTiltaksgjennomforingerV1 = KafkaTopicConsumer.Config(
                id = "siste-tiltaksgjennomforinger",
                topic = "siste-tiltaksgjennomforinger",
            ),
        ),
    ),
    clients = ClientConfig(
        tiltakDatadeling = ServiceClientConfig(url = "http://tiltak-datadeling", scope = "tiltak-datadeling"),
    ),
)

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
