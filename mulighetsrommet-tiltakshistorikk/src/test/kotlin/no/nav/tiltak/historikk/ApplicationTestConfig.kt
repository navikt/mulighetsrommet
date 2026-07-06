package no.nav.tiltak.historikk

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.kotest.extensions.createRandomDatabaseConfig
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.util.UUID

val databaseConfig: DatabaseConfig = createRandomDatabaseConfig("mr-tiltakshistorikk")

fun <R> withTestApplication(
    oauth: MockOAuth2Server = MockOAuth2Server(),
    httpClientEngine: HttpClientEngine = createMockEngine(),
    config: AppConfig = createTestApplicationConfig(oauth, httpClientEngine),
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
                json()
            }
        }

        test()
    }
}
val teamMulighetsrommetTestEntraAdGroupId = UUID.fromString("a0000000-0000-0000-0000-000000000000")

fun createTestApplicationConfig(oauth: MockOAuth2Server, engine: HttpClientEngine) = ApplicationConfigLocal.copy(
    httpClientEngine = engine,
    database = databaseConfig,
    auth = createAuthConfig(oauth),
)

// Default values for 'iss' og 'aud' in tokens issued by mock-oauth2-server is 'default'.
// These values are set as the default here so that standard tokens issued by MockOAuth2Server works with a minimal amount of setup.
fun createAuthConfig(
    oauth: MockOAuth2Server,
    issuer: String = "default",
    audience: String = "default",
    privateJwk: String = "azure",
): AuthConfig {
    return AuthConfig(
        azure = AuthProvider(
            issuer = oauth.issuerUrl(issuer).toString(),
            jwksUri = oauth.jwksUrl(issuer).toUri().toString(),
            audience = audience,
            tokenEndpointUrl = oauth.tokenEndpointUrl(issuer).toString(),
            privateJwk = privateJwk,
        ),
        texas = ApplicationConfigLocal.auth.texas,
        teamMulighetsrommetEntraAdGroupId = teamMulighetsrommetTestEntraAdGroupId,
    )
}
