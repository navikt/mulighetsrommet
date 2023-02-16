package no.nav.mulighetsrommet.api

import io.ktor.server.testing.*
import no.nav.mulighetsrommet.api.producers.TiltaksgjennomforingKafkaProducer
import no.nav.mulighetsrommet.api.producers.TiltakstypeKafkaProducer
import no.nav.mulighetsrommet.api.tasks.SynchronizeNorgEnheter
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
    kafka = createKafkaConfig(),
    sanity = createSanityConfig(),
    veilarboppfolgingConfig = createServiceClientConfig("veilarboppfolging"),
    veilarbvedtaksstotteConfig = createServiceClientConfig("veilarbvedtaksstotte"),
    veilarbpersonConfig = createServiceClientConfig("veilarbperson"),
    veilarbveilederConfig = createServiceClientConfig("veilarbveileder"),
    veilarbdialogConfig = createServiceClientConfig("veilarbdialog"),
    poaoTilgang = createServiceClientConfig("poaotilgang"),
    amtEnhetsregister = createServiceClientConfig("amtenhetsregister"),
    msGraphConfig = createServiceClientConfig("ms-graph"),
    arenaAdapter = createServiceClientConfig("arena-adapter"),
    tasks = TaskConfig(
        synchronizeNorgEnheter = SynchronizeNorgEnheter.Config(
            delayOfMinutes = 10
        ),
    ),
    norg2 = Norg2Config(baseUrl = "")
)

fun createKafkaConfig(): KafkaConfig {
    return KafkaConfig(
        brokerUrl = "localhost:29092",
        producerId = "producer-id",
        producers = KafkaProducers(
            tiltaksgjennomforinger = TiltaksgjennomforingKafkaProducer.Config(topic = "siste-tiltaksgjennomforinger-v1"),
            tiltakstyper = TiltakstypeKafkaProducer.Config(topic = "siste-tiltakstyper-v1")
        )
    )
}

fun createServiceClientConfig(url: String): ServiceClientConfig {
    return ServiceClientConfig(
        url = url,
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
