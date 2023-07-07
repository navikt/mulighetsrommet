package no.nav.mulighetsrommet.api

import io.ktor.server.testing.*
import no.nav.mulighetsrommet.api.clients.brreg.BrregClientImpl
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.producers.TiltaksgjennomforingKafkaProducer
import no.nav.mulighetsrommet.api.producers.TiltakstypeKafkaProducer
import no.nav.mulighetsrommet.api.tasks.*
import no.nav.mulighetsrommet.database.FlywayDatabaseAdapter
import no.nav.mulighetsrommet.database.kotest.extensions.createDatabaseTestSchema
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.security.mock.oauth2.MockOAuth2Server

var databaseConfig: FlywayDatabaseAdapter.Config? = null

fun createDatabaseTestConfig() =
    if (databaseConfig == null) {
        databaseConfig = createDatabaseTestSchema("mulighetsrommet-api-db", 5442)
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
    auth = createAuthConfig(oauth),
    kafka = createKafkaConfig(),
    sanity = SanityClient.Config(projectId = "", token = "", dataset = "", apiVersion = ""),
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
        deleteExpiredTiltakshistorikk = DeleteExpiredTiltakshistorikk.Config(disabled = true),
        synchronizeNorgEnheter = SynchronizeNorgEnheter.Config(
            delayOfMinutes = 10,
            disabled = true,
        ),
        synchronizeEnheterFraSanityTilApi = SynchronizeTiltaksgjennomforingEnheter.Config(
            delayOfMinutes = 10,
            disabled = true,
        ),
        synchronizeNavAnsatte = SynchronizeNavAnsatte.Config(
            disabled = true,
        ),
        notifySluttdatoForGjennomforingerNarmerSeg = NotifySluttdatoForGjennomforingerNarmerSeg.Config(
            disabled = true,
        ),
        notifySluttdatoForAvtalerNarmerSeg = NotifySluttdatoForAvtalerNarmerSeg.Config(disabled = true),
        notifyFailedKafkaEvents = NotifyFailedKafkaEvents.Config(
            disabled = true,
            delayOfMinutes = 15,
            maxRetries = 5,
        ),
        notifySluttdatoForMidlertidigStengtGjennomforingerNarmerSeg = NotifySluttdatoForMidlertidigStengtGjennomforingerNarmerSeg.Config(
            disabled = true,
        ),
        oppdaterMetrikker = OppdaterMetrikker.Config(
            disabled = true,
            delayOfMinutes = 10,
        ),
    ),
    norg2 = Norg2Config(baseUrl = ""),
    slack = SlackConfig(
        token = "",
        channel = "",
        enable = false,
    ),
    brreg = BrregClientImpl.Config(baseUrl = ""),
    navAnsattService = NavAnsattServiceConfig(roller = listOf()),
)

fun createKafkaConfig(): KafkaConfig {
    return KafkaConfig(
        brokerUrl = "localhost:29092",
        producerId = "mulighetsrommet-api-producer",
        producers = KafkaProducers(
            tiltaksgjennomforinger = TiltaksgjennomforingKafkaProducer.Config(topic = "siste-tiltaksgjennomforinger-v1"),
            tiltakstyper = TiltakstypeKafkaProducer.Config(topic = "siste-tiltakstyper-v1"),
        ),
        consumerGroupId = "mulighetsrommet-api-consumer",
        consumers = KafkaConsumers(
            amtDeltakerV1 = KafkaTopicConsumer.Config(id = "amt-deltaker", topic = "amt-deltaker"),
            amtVirksomheterV1 = KafkaTopicConsumer.Config(id = "amt-virksomheter", topic = "amt-virksomheter"),
        ),
    )
}

fun createServiceClientConfig(url: String): ServiceClientConfig {
    return ServiceClientConfig(
        url = url,
        scope = "",
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
