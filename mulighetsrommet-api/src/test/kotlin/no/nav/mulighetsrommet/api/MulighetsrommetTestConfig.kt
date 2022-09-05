package no.nav.mulighetsrommet.api

import io.ktor.server.testing.*
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.Password
import no.nav.security.mock.oauth2.MockOAuth2Server

fun <R> withMulighetsrommetApp(
    oauth: MockOAuth2Server = MockOAuth2Server(),
    config: AppConfig = createTestApplicationConfig(oauth),
    test: suspend ApplicationTestBuilder.() -> R
) {
    testApplication {
        application {
            configure(config)
        }
        test()
    }
}

fun createTestApplicationConfig(oauth: MockOAuth2Server) = AppConfig(
    database = createDatabaseConfigWithRandomSchema(),
    auth = createAuthConfig(oauth),
    sanity = createSanityConfig(),
    veilarboppfolgingConfig = createVeilarboppfolgingConfig(),
    veilarbvedtaksstotteConfig = createVeilarbvedsstotteConfig(),
    veilarbpersonConfig = createVeilarbpersonConfig(),
    veilarbveilederConfig = createVeilarbveilederConfig(),
    veilarbdialogConfig = createVeilarbdialogConfig(),
)

fun createVeilarboppfolgingConfig(): VeilarboppfolgingConfig {
    return VeilarboppfolgingConfig(
        url = "",
        scope = ""
    )
}

fun createVeilarbvedsstotteConfig(): VeilarbvedtaksstotteConfig {
    return VeilarbvedtaksstotteConfig(
        url = "",
        scope = ""
    )
}

fun createVeilarbpersonConfig(): VeilarbpersonConfig {
    return VeilarbpersonConfig(
        url = "",
        scope = ""
    )
}
fun createVeilarbveilederConfig(): VeilarbveilederConfig {
    return VeilarbveilederConfig(
        url = "",
        scope = ""
    )
}

fun createVeilarbdialogConfig(): VeilarbdialogConfig {
    return VeilarbdialogConfig(
        url = "",
        scope = ""
    )
}

fun createDatabaseConfigWithRandomSchema(
    host: String = "localhost",
    port: Int = 5442,
    name: String = "mulighetsrommet-api-db",
    user: String = "valp",
    password: Password = Password("valp"),
    maximumPoolSize: Int = 1,
): DatabaseConfig {
    val schema = "$name-${java.util.UUID.randomUUID()}"
    return DatabaseConfig(host, port, name, schema, user, password, maximumPoolSize)
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

fun createSanityConfig(): SanityConfig {
    return SanityConfig(
        projectId = "",
        authToken = "",
        dataset = ""
    )
}
