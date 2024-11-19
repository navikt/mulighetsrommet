package no.nav.mulighetsrommet.api

import io.ktor.server.application.*
import io.ktor.server.testing.*
import no.nav.mulighetsrommet.altinn.AltinnClient
import no.nav.mulighetsrommet.api.avtale.task.NotifySluttdatoForAvtalerNarmerSeg
import no.nav.mulighetsrommet.api.clients.brreg.BrregClient
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.gjennomforing.kafka.ArenaMigreringTiltaksgjennomforingerV1KafkaProducer
import no.nav.mulighetsrommet.api.gjennomforing.kafka.SisteTiltaksgjennomforingerV1KafkaProducer
import no.nav.mulighetsrommet.api.gjennomforing.task.NotifySluttdatoForGjennomforingerNarmerSeg
import no.nav.mulighetsrommet.api.gjennomforing.task.UpdateApentForPamelding
import no.nav.mulighetsrommet.api.navansatt.task.SynchronizeNavAnsatte
import no.nav.mulighetsrommet.api.navenhet.task.SynchronizeNorgEnheter
import no.nav.mulighetsrommet.api.refusjon.task.GenerateRefusjonskrav
import no.nav.mulighetsrommet.api.tasks.NotifyFailedKafkaEvents
import no.nav.mulighetsrommet.api.tiltakstype.kafka.SisteTiltakstyperV2KafkaProducer
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.database.kotest.extensions.createRandomDatabaseConfig
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.unleash.UnleashService
import no.nav.mulighetsrommet.utdanning.client.UtdanningClient
import no.nav.mulighetsrommet.utdanning.task.SynchronizeUtdanninger
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

        test()
    }
}

fun createTestApplicationConfig() = AppConfig(
    database = databaseConfig,
    flyway = FlywayMigrationManager.MigrationConfig(),
    auth = createAuthConfig(oauth = null, roles = listOf()),
    kafka = createKafkaConfig(),
    sanity = SanityClient.Config(projectId = "", token = "", dataset = "", apiVersion = ""),
    veilarboppfolgingConfig = createServiceClientConfig("veilarboppfolging"),
    veilarbvedtaksstotteConfig = createServiceClientConfig("veilarbvedtaksstotte"),
    veilarbdialogConfig = createServiceClientConfig("veilarbdialog"),
    amtDeltakerConfig = createServiceClientConfig("deltakelser"),
    poaoTilgang = createServiceClientConfig("poaotilgang"),
    msGraphConfig = createServiceClientConfig("ms-graph"),
    arenaAdapter = createServiceClientConfig("arena-adapter"),
    tiltakshistorikk = createServiceClientConfig("tiltakshistorikk"),
    tasks = TaskConfig(
        synchronizeNorgEnheter = SynchronizeNorgEnheter.Config(
            delayOfMinutes = 10,
            disabled = true,
        ),
        synchronizeNavAnsatte = SynchronizeNavAnsatte.Config(
            disabled = true,
        ),
        synchronizeUtdanninger = SynchronizeUtdanninger.Config(
            disabled = true,
        ),
        notifySluttdatoForGjennomforingerNarmerSeg = NotifySluttdatoForGjennomforingerNarmerSeg.Config(
            disabled = true,
        ),
        notifySluttdatoForAvtalerNarmerSeg = NotifySluttdatoForAvtalerNarmerSeg.Config(disabled = true),
        notifyFailedKafkaEvents = NotifyFailedKafkaEvents.Config(
            disabled = true,
            cronPattern = "",
            maxRetries = 5,
        ),
        updateApentForPamelding = UpdateApentForPamelding.Config(
            disabled = true,
        ),
        generateRefusjonskrav = GenerateRefusjonskrav.Config(
            disabled = true,
            cronPattern = null,
        ),
    ),
    norg2 = Norg2Config(baseUrl = ""),
    slack = SlackConfig(
        token = "",
        channel = "",
        enable = false,
    ),
    brreg = BrregClient.Config(baseUrl = "brreg"),
    unleash = UnleashService.Config(
        appName = "",
        url = "http://localhost:8090",
        token = "",
        instanceId = "",
        environment = "",
    ),
    axsys = ServiceClientConfig(url = "", scope = ""),
    pdl = ServiceClientConfig(url = "", scope = ""),
    pamOntologi = createServiceClientConfig("pam-ontologi"),
    utdanning = UtdanningClient.Config(
        baseUrl = "",
    ),
    altinn = AltinnClient.Config(
        url = "altinn",
        scope = "default",
        apiKey = "apiKey",
    ),
    isoppfolgingstilfelleConfig = ServiceClientConfig(
        url = "isoppfolgingstilfelleConfig",
        scope = "default",
    ),
)

fun createKafkaConfig(): KafkaConfig = KafkaConfig(
    brokerUrl = "localhost:29092",
    producerId = "mulighetsrommet-api-producer",
    producers = KafkaProducers(
        tiltaksgjennomforinger = SisteTiltaksgjennomforingerV1KafkaProducer.Config(topic = "siste-tiltaksgjennomforinger-v1"),
        tiltakstyper = SisteTiltakstyperV2KafkaProducer.Config(topic = "siste-tiltakstyper-v2"),
        arenaMigreringTiltaksgjennomforinger = ArenaMigreringTiltaksgjennomforingerV1KafkaProducer.Config(
            topic = "arena-migrering-tiltaksgjennomforinger-v1",
        ),
    ),
    defaultConsumerGroupId = "mulighetsrommet-api-consumer",
    consumers = KafkaConsumers(
        tiltaksgjennomforingerV1 = KafkaTopicConsumer.Config(
            id = "siste-tiltaksgjennomforinger",
            topic = "siste-tiltaksgjennomforinger-v1",
        ),
        amtDeltakerV1 = KafkaTopicConsumer.Config(id = "amt-deltaker", topic = "amt-deltaker"),
        amtVirksomheterV1 = KafkaTopicConsumer.Config(id = "amt-virksomheter", topic = "amt-virksomheter"),
    ),
)

fun createServiceClientConfig(url: String): ServiceClientConfig = ServiceClientConfig(
    url = url,
    scope = "",
)

// Default values for 'iss' og 'aud' in tokens issued by mock-oauth2-server is 'default'.
// These values are set as the default here so that standard tokens issued by MockOAuth2Server works with a minimal amount of setup.
fun createAuthConfig(
    oauth: MockOAuth2Server?,
    issuer: String = "default",
    audience: String = "default",
    roles: List<AdGruppeNavAnsattRolleMapping>,
): AuthConfig = AuthConfig(
    azure = AuthProvider(
        issuer = oauth?.issuerUrl(issuer)?.toString() ?: issuer,
        audience = audience,
        jwksUri = oauth?.jwksUrl(issuer)?.toUri()?.toString() ?: "http://localhost",
        tokenEndpointUrl = oauth?.tokenEndpointUrl(issuer)?.toString() ?: "http://localhost",
    ),
    roles = roles,
    tokenx = AuthProvider(
        issuer = oauth?.issuerUrl(issuer)?.toString() ?: issuer,
        audience = audience,
        jwksUri = oauth?.jwksUrl(issuer)?.toUri()?.toString() ?: "http://localhost",
        tokenEndpointUrl = oauth?.tokenEndpointUrl(issuer)?.toString() ?: "http://localhost",
    ),
    maskinporten = AuthProvider(
        issuer = oauth?.issuerUrl(issuer)?.toString() ?: issuer,
        audience = audience,
        jwksUri = oauth?.jwksUrl(issuer)?.toUri()?.toString() ?: "http://localhost",
        tokenEndpointUrl = oauth?.tokenEndpointUrl(issuer)?.toString() ?: "http://localhost",
    ),
)
