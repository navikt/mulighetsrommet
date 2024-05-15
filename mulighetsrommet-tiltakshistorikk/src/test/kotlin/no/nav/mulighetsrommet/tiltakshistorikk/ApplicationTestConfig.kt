package no.nav.mulighetsrommet.tiltakshistorikk

import io.ktor.server.testing.*
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.database.kotest.extensions.createDatabaseTestSchema
import no.nav.security.mock.oauth2.MockOAuth2Server

var databaseConfig: DatabaseConfig? = null
fun createDatabaseTestConfig() =
    if (databaseConfig == null) {
        databaseConfig = createDatabaseTestSchema("mr-tiltakshistorikk")
        databaseConfig!!
    } else {
        databaseConfig!!
    }

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
    database = createDatabaseTestConfig(),
    flyway = FlywayMigrationManager.MigrationConfig(),
    auth = createAuthConfig(oauth),
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
