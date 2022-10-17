package no.nav.mulighetsrommet.api

import io.ktor.server.testing.*
import no.nav.mulighetsrommet.database.kotest.extensions.createApiDatabaseTestSchema
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
    database = createApiDatabaseTestSchema(),
    auth = createAuthConfig(oauth),
    sanity = createSanityConfig(),
    veilarboppfolgingConfig = createVeilarboppfolgingConfig(),
    veilarbvedtaksstotteConfig = createVeilarbvedsstotteConfig(),
    veilarbpersonConfig = createVeilarbpersonConfig(),
    veilarbveilederConfig = createVeilarbveilederConfig(),
    veilarbdialogConfig = createVeilarbdialogConfig(),
    veilarbarenaConfig = createVeilarbarenaConfig(),
    poaoGcpProxy = createPoaoGcpProxyConfig(),
    poaoTilgang = PoaoTilgangConfig("", ""),
    amtEnhetsregister = createAmtEnhetsregisterConfig(),
    arenaOrdsProxy = createArenaOrdsProxyConfig()
)

fun createVeilarbarenaConfig(): VeilarbvarenaConfig {
    return VeilarbvarenaConfig(
        url = "",
        scope = ""
    )
}

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

fun createPoaoGcpProxyConfig(): PoaoGcpProxyConfig {
    return PoaoGcpProxyConfig(
        url = "",
        scope = ""
    )
}

fun createAmtEnhetsregisterConfig(): AmtEnhetsregisterConfig {
    return AmtEnhetsregisterConfig(
        url = "",
        scope = ""
    )
}

fun createArenaOrdsProxyConfig(): ArenaOrdsProxyConfig {
    return ArenaOrdsProxyConfig(
        url = "",
        scope = ""
    )
}
