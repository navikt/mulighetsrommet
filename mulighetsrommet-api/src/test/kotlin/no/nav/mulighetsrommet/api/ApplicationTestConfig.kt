package no.nav.mulighetsrommet.api

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import no.nav.common.kafka.util.KafkaPropertiesBuilder.consumerBuilder
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.avtale.task.NotifySluttdatoForAvtalerNarmerSeg
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.gjennomforing.task.NotifySluttdatoForGjennomforingerNarmerSeg
import no.nav.mulighetsrommet.api.gjennomforing.task.UpdateApentForPamelding
import no.nav.mulighetsrommet.api.navansatt.task.SynchronizeNavAnsatte
import no.nav.mulighetsrommet.api.navenhet.task.SynchronizeNorgEnheter
import no.nav.mulighetsrommet.api.tiltakstype.service.TiltakstypeService
import no.nav.mulighetsrommet.api.utbetaling.service.tidligstTidspunktForUtbetalingDev
import no.nav.mulighetsrommet.api.utbetaling.task.GenerateUtbetaling
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.database.kotest.extensions.createRandomDatabaseConfig
import no.nav.mulighetsrommet.featuretoggle.service.UnleashFeatureToggleService
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.tokenprovider.TexasClient
import no.nav.mulighetsrommet.tokenprovider.TokenReponse
import no.nav.mulighetsrommet.utdanning.task.SynchronizeUtdanninger
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.ByteArraySerializer
import java.time.LocalDate

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
                contentType(ContentType.Application.Json)
            }
        }

        test()
    }
}

fun createTestApplicationConfig() = ApplicationConfigTest

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
    texas = TexasClient.Config(
        tokenEndpoint = "http://localhost/api/v1/token",
        tokenExchangeEndpoint = "http://localhost/api/v1/token/exchange",
        tokenIntrospectionEndpoint = "http://localhost/api/v1/introspect",
        engine = MockEngine { _ ->
            respond(
                content = Json.encodeToString(
                    TokenReponse(
                        access_token = "dummy",
                        token_type = TokenReponse.TokenType.Bearer,
                        expires_in = 1_000_000,
                    ),
                ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        },
    ),
)

val ApplicationConfigTest = AppConfig(
    tiltakstyper = TiltakstypeService.Config(),
    okonomi = OkonomiConfig(
        gyldigTilsagnPeriode = Tiltakskode.entries.associateWith {
            Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2030, 1, 1))
        },
        opprettKravPrismodeller = listOf(
            PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK,
            PrismodellType.ANNEN_AVTALT_PRIS,
            PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER,
        ),
        tidligstTidspunktForUtbetaling = tidligstTidspunktForUtbetalingDev,
    ),
    database = databaseConfig,
    flyway = FlywayMigrationManager.MigrationConfig(),
    kafka = KafkaConfig(
        producerProperties = KafkaPropertiesBuilder.producerBuilder()
            .withBaseProperties()
            .withProducerId("mulighetsrommet-api-kafka-producer.test")
            .withBrokerUrl("localhost:29092")
            .withSerializers(ByteArraySerializer::class.java, ByteArraySerializer::class.java)
            .build(),
        clients = KafkaClients(
            { consumerGroupId ->
                consumerBuilder()
                    .withBaseProperties()
                    .withConsumerGroupId(consumerGroupId)
                    .withBrokerUrl("localhost:29092")
                    .withDeserializers(ByteArrayDeserializer::class.java, ByteArrayDeserializer::class.java)
                    .build()
            },
        ),
        topics = KafkaTopics(
            okonomiBestillingTopic = "test.tiltaksokonomi.bestillinger-v1",
            sisteTiltaksgjennomforingerV2Topic = "test.siste-tiltaksgjennomforinger-v2",
            sisteTiltakstyperTopic = "test.siste-tiltakstyper-v3",
            arenaMigreringGjennomforingTopic = "test.arena-migrering-tiltaksgjennomforinger-v1",
            datavarehusTiltakTopic = "test.datavarehus-tiltak-v1",
            helvedUtbetalingTopic = "test.tilskudd.utbetaling-v1",
            totrinnskontrollTopic = "test.totrinnskontroll-v1",
        ),
    ),
    auth = createAuthConfig(oauth = null, roles = setOf()),
    sanity = SanityClient.Config(
        dataset = "test",
        projectId = "test",
        token = "",
        useCdn = false,
    ),
    slack = SlackConfig(
        token = "",
        channel = "",
        enable = false,
    ),
    unleash = UnleashFeatureToggleService.Config(
        appName = "mulighetsrommet-api",
        url = "http://localhost/unleash",
        token = "",
        instanceId = "mulighetsrommet-api",
        environment = "test",
    ),
    tilgangsmaskin = AuthenticatedHttpClientConfig(
        url = "http://localhost/tilgangsmaskin",
        scope = "default",
    ),
    veilarboppfolgingConfig = AuthenticatedHttpClientConfig(
        url = "http://localhost/veilarboppfolging/api",
        scope = "default",
    ),
    veilarbvedtaksstotteConfig = AuthenticatedHttpClientConfig(
        url = "http://localhost/veilarbvedtaksstotte/api",
        scope = "default",
    ),
    veilarbdialogConfig = AuthenticatedHttpClientConfig(
        url = "http://localhost/veilarbdialog/api",
        scope = "default",
    ),
    amtDeltakerConfig = AuthenticatedHttpClientConfig(
        url = "http://localhost/amt-deltaker",
        scope = "default",
    ),
    poaoTilgang = AuthenticatedHttpClientConfig(
        url = "http://localhost/poao-tilgang",
        scope = "default",
    ),
    arenaAdapter = AuthenticatedHttpClientConfig(
        url = "http://localhost/arena-adapter",
        scope = "default",
    ),
    tiltakshistorikk = AuthenticatedHttpClientConfig(
        url = "http://localhost/tiltakshistorikk",
        scope = "default",
    ),
    pdfgen = HttpClientConfig(
        url = "http://localhost/pdfgen",
    ),
    msGraphConfig = AuthenticatedHttpClientConfig(
        url = "http://localhost/ms-graph",
        scope = "default",
    ),
    isoppfolgingstilfelleConfig = AuthenticatedHttpClientConfig(
        url = "http://localhost/isoppfolgingstilfelle",
        scope = "default",
    ),
    norg2 = HttpClientConfig(
        url = "http://localhost/norg2",
    ),
    pamOntologi = AuthenticatedHttpClientConfig(
        url = "http://localhost/pam-ontologi",
        scope = "default",
    ),
    pdl = AuthenticatedHttpClientConfig(
        url = "http://localhost/pdl",
        scope = "default",
    ),
    utdanning = HttpClientConfig(
        url = "http://localhost/utdanning",
    ),
    altinn = AuthenticatedHttpClientConfig(
        url = "http://localhost/altinn",
        scope = "default",
    ),
    dokark = AuthenticatedHttpClientConfig(
        url = "http://localhost/dokark",
        scope = "default",
    ),
    dokdistfordeling = AuthenticatedHttpClientConfig(
        url = "http://localhost/dokdistfordeling",
        scope = "default",
    ),
    kontoregisterOrganisasjon = AuthenticatedHttpClientConfig(
        url = "http://localhost/kontoregister",
        scope = "default",
    ),
    clamav = HttpClientConfig(
        url = "http://localhost",
    ),
    tasks = TaskConfig(
        synchronizeNorgEnheter = SynchronizeNorgEnheter.Config(disabled = true),
        synchronizeNavAnsatte = SynchronizeNavAnsatte.Config(disabled = true),
        synchronizeUtdanninger = SynchronizeUtdanninger.Config(disabled = true),
        notifySluttdatoForGjennomforingerNarmerSeg = NotifySluttdatoForGjennomforingerNarmerSeg.Config(disabled = true),
        notifySluttdatoForAvtalerNarmerSeg = NotifySluttdatoForAvtalerNarmerSeg.Config(disabled = true),
        updateApentForPamelding = UpdateApentForPamelding.Config(disabled = true),
        generateUtbetaling = GenerateUtbetaling.Config(disabled = true),
    ),
)
