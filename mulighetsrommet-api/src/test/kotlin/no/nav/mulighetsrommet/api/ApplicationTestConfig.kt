package no.nav.mulighetsrommet.api

import io.ktor.server.application.*
import io.ktor.server.testing.*
import no.nav.mulighetsrommet.api.clients.brreg.BrregClient
import no.nav.mulighetsrommet.api.clients.pamOntologi.PamOntologiClient
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.tasks.*
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.database.kotest.extensions.createDatabaseTestSchema
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.producers.ArenaMigreringTiltaksgjennomforingKafkaProducer
import no.nav.mulighetsrommet.kafka.producers.TiltaksgjennomforingKafkaProducer
import no.nav.mulighetsrommet.kafka.producers.TiltakstypeKafkaProducer
import no.nav.mulighetsrommet.unleash.UnleashService
import no.nav.security.mock.oauth2.MockOAuth2Server

var databaseConfig: DatabaseConfig? = null

fun createDatabaseTestConfig() =
    if (databaseConfig == null) {
        databaseConfig = createDatabaseTestSchema("mr-api")
        databaseConfig!!
    } else {
        databaseConfig!!
    }

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
    database = createDatabaseTestConfig(),
    flyway = FlywayMigrationManager.MigrationConfig(),
    auth = createAuthConfig(oauth = null, roles = listOf()),
    kafka = createKafkaConfig(),
    sanity = SanityClient.Config(projectId = "", token = "", dataset = "", apiVersion = ""),
    veilarboppfolgingConfig = createServiceClientConfig("veilarboppfolging"),
    veilarbvedtaksstotteConfig = createServiceClientConfig("veilarbvedtaksstotte"),
    veilarbveilederConfig = createServiceClientConfig("veilarbveileder"),
    veilarbdialogConfig = createServiceClientConfig("veilarbdialog"),
    amtDeltakerConfig = createServiceClientConfig("deltakelser"),
    poaoTilgang = createServiceClientConfig("poaotilgang"),
    msGraphConfig = createServiceClientConfig("ms-graph"),
    arenaAdapter = createServiceClientConfig("arena-adapter"),
    tasks = TaskConfig(
        deleteExpiredTiltakshistorikk = DeleteExpiredTiltakshistorikk.Config(disabled = true),
        synchronizeNorgEnheter = SynchronizeNorgEnheter.Config(
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
            cronPattern = "",
            maxRetries = 5,
        ),
        updateApentForInnsok = UpdateApentForInnsok.Config(
            disabled = true,
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
        url = "",
        token = "",
        instanceId = "",
        environment = "",
    ),
    axsys = ServiceClientConfig(url = "", scope = ""),
    pdl = ServiceClientConfig(url = "", scope = ""),
    migrerteTiltak = emptyList(),
    pamOntologi = PamOntologiClient.Config(baseUrl = "pam-ontologi"),
)

fun createKafkaConfig(): KafkaConfig = KafkaConfig(
    brokerUrl = "localhost:29092",
    producerId = "mulighetsrommet-api-producer",
    producers = KafkaProducers(
        tiltaksgjennomforinger = TiltaksgjennomforingKafkaProducer.Config(topic = "siste-tiltaksgjennomforinger-v1"),
        tiltakstyper = TiltakstypeKafkaProducer.Config(topic = "siste-tiltakstyper-v2"),
        arenaMigreringTiltaksgjennomforinger = ArenaMigreringTiltaksgjennomforingKafkaProducer.Config(
            topic = "arena-migrering-tiltaksgjennomforinger-v1",
        ),
    ),
    consumerGroupId = "mulighetsrommet-api-consumer",
    consumers = KafkaConsumers(
        tiltaksgjennomforingerV1 = KafkaTopicConsumer.Config(
            id = "siste-tiltaksgjennomforinger",
            topic = "siste-tiltaksgjennomforinger-v1",
        ),
        amtDeltakerV1 = KafkaTopicConsumer.Config(id = "amt-deltaker", topic = "amt-deltaker"),
        amtVirksomheterV1 = KafkaTopicConsumer.Config(id = "amt-virksomheter", topic = "amt-virksomheter"),
        ptoSisteOppfolgingsperiodeV1 = KafkaTopicConsumer.Config(
            id = "pto-sisteoppfolgingsperiode",
            topic = "pto-sisteoppfolgingsperiode",
        ),
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
)
