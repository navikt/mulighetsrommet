package no.nav.mulighetsrommet.api

import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.database.kotest.extensions.createRandomDatabaseConfig
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.security.mock.oauth2.MockOAuth2Server

val databaseConfig: DatabaseConfig = createRandomDatabaseConfig("mr-api")

fun <R> withTestApplication(
    config: AppConfig = createTestApplicationConfig(),
    additionalConfiguration: (Application.() -> Unit)? = null,
    test: suspend ApplicationTestBuilder.() -> R,
) {
    testApplication {
        application {
            configure(config)

            additionalConfiguration?.invoke(this)
        }

        client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            install(DefaultRequest) {
                contentType(Json)
            }
        }

        test()
    }
}

fun kafkaTestConfig(kafkaConfig: KafkaConfig): KafkaConfig {
    val testification = { str: String -> "$str-test" }
    return kafkaConfig.copy(
        topics = KafkaTopics(
            okonomiBestillingTopic = testification(kafkaConfig.topics.okonomiBestillingTopic),
            sisteTiltaksgjennomforingerV2Topic = testification(kafkaConfig.topics.sisteTiltaksgjennomforingerV2Topic),
            sisteTiltakstyperTopic = testification(kafkaConfig.topics.sisteTiltakstyperTopic),
            arenaMigreringGjennomforingTopic = testification(kafkaConfig.topics.arenaMigreringGjennomforingTopic),
            datavarehusTiltakTopic = testification(kafkaConfig.topics.datavarehusTiltakTopic),
        ),
    )
}

fun createTestApplicationConfig() = ApplicationConfigLocal.copy(
    engine = createMockEngine(),
    database = databaseConfig,
    flyway = FlywayMigrationManager.MigrationConfig(),
    auth = createAuthConfig(oauth = null, roles = setOf()),
    kafka = kafkaTestConfig(ApplicationConfigLocal.kafka),
)

// Default values for 'iss' og 'aud' in tokens issued by mock-oauth2-server is 'default'.
// These values are set as the default here so that standard tokens issued by MockOAuth2Server works with a minimal amount of setup.
fun createAuthConfig(
    oauth: MockOAuth2Server?,
    issuer: String = "default",
    audience: String = "default",
    roles: Set<EntraGroupNavAnsattRolleMapping>,
): AuthConfig = AuthConfig(
    azure = AuthProvider(
        issuer = oauth?.issuerUrl(issuer)?.toString() ?: issuer,
        audience = audience,
        jwksUri = oauth?.jwksUrl(issuer)?.toUri()?.toString() ?: "http://localhost",
        tokenEndpointUrl = oauth?.tokenEndpointUrl(issuer)?.toString() ?: "http://localhost",
        privateJwk = "azure",
    ),
    roles = roles,
    tokenx = AuthProvider(
        issuer = oauth?.issuerUrl(issuer)?.toString() ?: issuer,
        audience = audience,
        jwksUri = oauth?.jwksUrl(issuer)?.toUri()?.toString() ?: "http://localhost",
        tokenEndpointUrl = oauth?.tokenEndpointUrl(issuer)?.toString() ?: "http://localhost",
        privateJwk = "tokenx",
    ),
    maskinporten = AuthProvider(
        issuer = oauth?.issuerUrl(issuer)?.toString() ?: issuer,
        audience = audience,
        jwksUri = oauth?.jwksUrl(issuer)?.toUri()?.toString() ?: "http://localhost",
        tokenEndpointUrl = oauth?.tokenEndpointUrl(issuer)?.toString() ?: "http://localhost",
        privateJwk = "maskinporten",
    ),
    texas = ApplicationConfigLocal.auth.texas,
)
