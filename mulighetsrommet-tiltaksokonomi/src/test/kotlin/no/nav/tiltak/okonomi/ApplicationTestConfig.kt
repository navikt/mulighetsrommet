package no.nav.tiltak.okonomi

import io.ktor.client.engine.*
import io.ktor.server.testing.*
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.database.kotest.extensions.createRandomDatabaseConfig
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.security.mock.oauth2.MockOAuth2Server

val databaseConfig: DatabaseConfig = createRandomDatabaseConfig("mr-tiltaksokonomi")

fun <R> withTestApplication(
    oauth: MockOAuth2Server = MockOAuth2Server(),
    httpClientEngine: HttpClientEngine = createMockEngine(),
    config: AppConfig = createTestApplicationConfig(httpClientEngine, createAuthConfig(oauth), databaseConfig),
    test: suspend ApplicationTestBuilder.() -> R,
) {
    testApplication {
        application {
            configure(config)
        }

        test()
    }
}

fun createTestApplicationConfig(
    engine: HttpClientEngine,
    auth: AuthConfig,
    database: DatabaseConfig,
): AppConfig = AppConfig(
    httpClientEngine = engine,
    database = database,
    flyway = FlywayMigrationManager.MigrationConfig(),
    auth = auth,
    clients = ClientConfig(
        oebsTiltakApi = AuthenticatedHttpClientConfig(url = "http://oebs-tiltak-api", scope = "default"),
    ),
    kafka = KafkaConfig(
        brokerUrl = "localhost:29092",
        defaultConsumerGroupId = "1",
        clients = KafkaClients(
            okonomiBestillingConsumer = KafkaTopicConsumer.Config(
                id = "okonomi-bestilling",
                topic = "okonomi-bestilling",
            ),
        ),
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
