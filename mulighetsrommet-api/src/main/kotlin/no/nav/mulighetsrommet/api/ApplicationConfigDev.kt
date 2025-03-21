package no.nav.mulighetsrommet.api

import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.utils.io.*
import no.nav.common.kafka.util.KafkaPropertiesPreset
import no.nav.mulighetsrommet.api.avtale.task.NotifySluttdatoForAvtalerNarmerSeg
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.datavarehus.kafka.DatavarehusTiltakV1KafkaProducer
import no.nav.mulighetsrommet.api.gjennomforing.kafka.ArenaMigreringTiltaksgjennomforingerV1KafkaProducer
import no.nav.mulighetsrommet.api.gjennomforing.kafka.SisteTiltaksgjennomforingerV1KafkaProducer
import no.nav.mulighetsrommet.api.gjennomforing.task.NotifySluttdatoForGjennomforingerNarmerSeg
import no.nav.mulighetsrommet.api.gjennomforing.task.UpdateApentForPamelding
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattRolle
import no.nav.mulighetsrommet.api.navansatt.task.SynchronizeNavAnsatte
import no.nav.mulighetsrommet.api.navenhet.task.SynchronizeNorgEnheter
import no.nav.mulighetsrommet.api.tasks.GenerateValidationReport
import no.nav.mulighetsrommet.api.tasks.NotifyFailedKafkaEvents
import no.nav.mulighetsrommet.api.tilsagn.OkonomiBestillingService
import no.nav.mulighetsrommet.api.tiltakstype.kafka.SisteTiltakstyperV2KafkaProducer
import no.nav.mulighetsrommet.api.utbetaling.task.GenerateUtbetaling
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.unleash.UnleashService
import no.nav.mulighetsrommet.utdanning.task.SynchronizeUtdanninger
import no.nav.mulighetsrommet.utils.toUUID
import java.time.LocalDate

val ApplicationConfigDev = AppConfig(
    database = DatabaseConfig(
        jdbcUrl = System.getenv("DB_JDBC_URL"),
        maximumPoolSize = 10,
    ),
    flyway = FlywayMigrationManager.MigrationConfig(
        strategy = FlywayMigrationManager.InitializationStrategy.Migrate,
    ),
    kafka = KafkaConfig(
        producerProperties = KafkaPropertiesPreset.aivenDefaultProducerProperties("mulighetsrommet-api-kafka-producer.v1"),
        consumerPreset = KafkaPropertiesPreset.aivenDefaultConsumerProperties("mulighetsrommet-api-kafka-consumer.v1"),
        producers = KafkaProducers(
            gjennomforinger = SisteTiltaksgjennomforingerV1KafkaProducer.Config(
                topic = "team-mulighetsrommet.siste-tiltaksgjennomforinger-v1",
            ),
            arenaMigreringTiltaksgjennomforinger = ArenaMigreringTiltaksgjennomforingerV1KafkaProducer.Config(
                topic = "team-mulighetsrommet.arena-migrering-tiltaksgjennomforinger-v1",
            ),
            tiltakstyper = SisteTiltakstyperV2KafkaProducer.Config(
                topic = "team-mulighetsrommet.siste-tiltakstyper-v2",
            ),
        ),
        clients = KafkaClients(
            dvhGjennomforing = DatavarehusTiltakV1KafkaProducer.Config(
                consumerId = "dvh-gjennomforing-consumer",
                consumerGroupId = "mulighetsrommet-api.datavarehus-gjennomforing.v1",
                consumerTopic = "team-mulighetsrommet.siste-tiltaksgjennomforinger-v1",
                producerTopic = "team-mulighetsrommet.datavarehus-tiltak-v1",
            ),
            okonomiBestilling = OkonomiBestillingService.Config(
                topic = "team-mulighetsrommet.tiltaksokonomi.bestillinger-v1",
            ),
        ),
        consumers = KafkaConsumers(
            gjennomforingerV1 = KafkaTopicConsumer.Config(
                id = "siste-tiltaksgjennomforinger",
                topic = "team-mulighetsrommet.siste-tiltaksgjennomforinger-v1",
            ),
            amtDeltakerV1 = KafkaTopicConsumer.Config(
                id = "amt-deltaker",
                topic = "amt.deltaker-v1",
                consumerGroupId = "mulighetsrommet-api.deltaker.v2",
            ),
            amtVirksomheterV1 = KafkaTopicConsumer.Config(
                id = "amt-virksomheter",
                topic = "amt.virksomheter-v1",
            ),
            amtArrangorMeldingV1 = KafkaTopicConsumer.Config(
                id = "amt-arrangor-melding",
                topic = "amt.arrangor-melding-v1",
            ),
            amtKoordinatorMeldingV1 = KafkaTopicConsumer.Config(
                id = "amt-tiltakskoordinators-deltakerliste",
                topic = "amt.tiltakskoordinators-deltakerliste-v1",
            ),
            replicateBestillingStatus = KafkaTopicConsumer.Config(
                id = "replicate-bestilling-status",
                topic = "team-mulighetsrommet.tiltaksokonomi.bestilling-status-v1",
            ),
            replicateFakturaStatus = KafkaTopicConsumer.Config(
                id = "replicate-faktura-status",
                topic = "team-mulighetsrommet.tiltaksokonomi.faktura-status-v1",
            ),
        ),
    ),
    auth = AuthConfig(
        azure = AuthProvider(
            issuer = System.getenv("AZURE_OPENID_CONFIG_ISSUER"),
            jwksUri = System.getenv("AZURE_OPENID_CONFIG_JWKS_URI"),
            audience = System.getenv("AZURE_APP_CLIENT_ID"),
            tokenEndpointUrl = System.getenv("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
            privateJwk = System.getenv("AZURE_APP_JWK"),
        ),
        tokenx = AuthProvider(
            issuer = System.getenv("TOKEN_X_ISSUER"),
            jwksUri = System.getenv("TOKEN_X_JWKS_URI"),
            audience = System.getenv("TOKEN_X_CLIENT_ID"),
            tokenEndpointUrl = System.getenv("TOKEN_X_WELL_KNOWN_URL"),
            privateJwk = System.getenv("TOKEN_X_PRIVATE_JWK"),
        ),
        maskinporten = AuthProvider(
            issuer = System.getenv("MASKINPORTEN_ISSUER"),
            jwksUri = System.getenv("MASKINPORTEN_JWKS_URI"),
            audience = System.getenv("MASKINPORTEN_CLIENT_ID"),
            tokenEndpointUrl = System.getenv("MASKINPORTEN_TOKEN_ENDPOINT"),
            privateJwk = System.getenv("MASKINPORTEN_CLIENT_JWK"),
        ),
        roles = listOf(
            AdGruppeNavAnsattRolleMapping(
                adGruppeId = "639e2806-4cc2-484c-a72a-51b4308c52a1".toUUID(),
                rolle = NavAnsattRolle.TEAM_MULIGHETSROMMET,
            ),
            AdGruppeNavAnsattRolleMapping(
                adGruppeId = "639e2806-4cc2-484c-a72a-51b4308c52a1".toUUID(),
                rolle = NavAnsattRolle.TILTAKADMINISTRASJON_GENERELL,
            ),
            AdGruppeNavAnsattRolleMapping(
                adGruppeId = "52bb9196-b071-4cc7-9472-be4942d33c4b".toUUID(),
                rolle = NavAnsattRolle.TILTAKADMINISTRASJON_GENERELL,
            ),
            AdGruppeNavAnsattRolleMapping(
                adGruppeId = "279039a0-39fd-4860-afdd-a1a2ccaa6323".toUUID(),
                rolle = NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV,
            ),
            AdGruppeNavAnsattRolleMapping(
                adGruppeId = "48026f54-6259-4c35-a148-bc4257bcaf03".toUUID(),
                rolle = NavAnsattRolle.AVTALER_SKRIV,
            ),
            AdGruppeNavAnsattRolleMapping(
                adGruppeId = "d9f317a1-2444-4fcd-b696-df8dbd6cc942".toUUID(),
                rolle = NavAnsattRolle.TILTAKADMINISTRASJON_ENDRINGSMELDING,
            ),
            AdGruppeNavAnsattRolleMapping(
                adGruppeId = "7b1d209a-f6c1-4c6e-84f2-02a1bb4c92ba".toUUID(),
                rolle = NavAnsattRolle.KONTAKTPERSON,
            ),
            AdGruppeNavAnsattRolleMapping(
                adGruppeId = "b00ba197-c90a-4ff9-966e-6c9cf1c882bf".toUUID(),
                rolle = NavAnsattRolle.OKONOMI_BESLUTTER,
            ),
        ),
    ),
    sanity = SanityClient.Config(
        dataset = System.getenv("SANITY_DATASET"),
        projectId = System.getenv("SANITY_PROJECT_ID"),
        token = System.getenv("SANITY_AUTH_TOKEN"),
    ),
    veilarboppfolgingConfig = AuthenticatedHttpClientConfig(
        url = "http://veilarboppfolging.poao/veilarboppfolging/api",
        scope = "api://dev-gcp.poao.veilarboppfolging/.default",
    ),
    veilarbvedtaksstotteConfig = AuthenticatedHttpClientConfig(
        url = "http://veilarbvedtaksstotte.obo/veilarbvedtaksstotte/api",
        scope = "api://dev-gcp.obo.veilarbvedtaksstotte/.default",
    ),
    veilarbdialogConfig = AuthenticatedHttpClientConfig(
        url = "http://veilarbdialog.dab/veilarbdialog/api",
        scope = "api://dev-gcp.dab.veilarbdialog/.default",
    ),
    amtDeltakerConfig = AuthenticatedHttpClientConfig(
        url = "http://amt-deltaker.amt",
        scope = "api://dev-gcp.amt.amt-deltaker/.default",
    ),
    poaoTilgang = AuthenticatedHttpClientConfig(
        url = "http://poao-tilgang.poao",
        scope = "api://dev-gcp.poao.poao-tilgang/.default",
    ),
    arenaAdapter = AuthenticatedHttpClientConfig(
        url = "http://mulighetsrommet-arena-adapter",
        scope = "api://dev-gcp.team-mulighetsrommet.mulighetsrommet-arena-adapter/.default",
    ),
    tiltakshistorikk = AuthenticatedHttpClientConfig(
        url = "http://tiltakshistorikk",
        scope = "api://dev-gcp.team-mulighetsrommet.tiltakshistorikk/.default",
    ),
    pdfgen = HttpClientConfig(url = "http://pdfgen"),
    msGraphConfig = AuthenticatedHttpClientConfig(
        url = "https://graph.microsoft.com",
        scope = "https://graph.microsoft.com/.default",
    ),
    isoppfolgingstilfelleConfig = AuthenticatedHttpClientConfig(
        url = "http://isoppfolgingstilfelle.teamsykefravr",
        scope = "api://dev-gcp.teamsykefravr.isoppfolgingstilfelle/.default",
    ),
    norg2 = HttpClientConfig(url = "http://norg2.org"),
    pamOntologi = AuthenticatedHttpClientConfig(
        url = "http://pam-ontologi.teampam",
        scope = "api://dev-gcp.teampam.pam-ontologi/.default",
    ),
    unleash = UnleashService.Config(
        appName = System.getenv("NAIS_APP_NAME"),
        url = System.getenv("UNLEASH_SERVER_API_URL"),
        token = System.getenv("UNLEASH_SERVER_API_TOKEN"),
        instanceId = System.getenv("NAIS_CLIENT_ID"),
        environment = "development",
    ),
    axsys = AuthenticatedHttpClientConfig(
        url = "https://axsys.dev-fss-pub.nais.io",
        scope = "api://dev-fss.org.axsys/.default",
    ),
    pdl = AuthenticatedHttpClientConfig(
        url = "https://pdl-api.dev-fss-pub.nais.io",
        scope = "api://dev-fss.pdl.pdl-api/.default",
    ),
    utdanning = HttpClientConfig(
        url = "https://api.utdanning.no",
    ),
    altinn = AuthenticatedHttpClientConfig(
        url = "https://platform.tt02.altinn.no",
        scope = System.getenv("MASKINPORTEN_SCOPES"),
    ),
    dokark = AuthenticatedHttpClientConfig(
        url = "https://dokarkiv-q2.dev-fss-pub.nais.io",
        scope = "api://dev-fss.teamdokumenthandtering.dokarkiv/.default",
    ),
    kontoregisterOrganisasjon = AuthenticatedHttpClientConfig(
        /**
         * Vi mocker ut kontoregisteret fordi q2-miljøet til kontoregisteret benytter fiktive organisasjoner, mens vår app benytter reelle fra Brreg
         * */
        engine = MockEngine { _ ->
            respond(
                content = ByteReadChannel(
                    """
                    {
                        "mottaker": "973674471",
                        "kontonr": "63728787114"
                    }
                    """.trimIndent(),
                ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        },
        url = "https://sokos-kontoregister-q2.dev-fss-pub.nais.io",
        scope = "api://dev-fss.okonomi.sokos-kontoregister-q2/.default",
    ),
    tasks = TaskConfig(
        synchronizeNorgEnheter = SynchronizeNorgEnheter.Config(
            delayOfMinutes = 360,
        ),
        synchronizeNavAnsatte = SynchronizeNavAnsatte.Config(
            cronPattern = "0 0 6 * * *",
        ),
        synchronizeUtdanninger = SynchronizeUtdanninger.Config(
            cronPattern = "0 0 6 * * *",
        ),
        notifySluttdatoForGjennomforingerNarmerSeg = NotifySluttdatoForGjennomforingerNarmerSeg.Config(
            cronPattern = "0 0 6 * * *",
        ),
        notifySluttdatoForAvtalerNarmerSeg = NotifySluttdatoForAvtalerNarmerSeg.Config(
            cronPattern = "0 0 6 * * *",
        ),
        notifyFailedKafkaEvents = NotifyFailedKafkaEvents.Config(
            maxRetries = 5,
            cronPattern = "0 */15 * ? * MON-FRI",
        ),
        generateValidationReport = GenerateValidationReport.Config(
            bucketName = "mulighetsrommet-api-uploads-dev",
        ),
        updateApentForPamelding = UpdateApentForPamelding.Config(
            disabled = false,
            cronPattern = "0 55 23 * * *",
        ),
        generateUtbetaling = GenerateUtbetaling.Config(
            cronPattern = "0 0 5 7 * *",
        ),
    ),
    slack = SlackConfig(
        token = System.getenv("SLACK_TOKEN"),
        channel = "#team-valp-monitorering-dev",
        enable = true,
    ),
    okonomi = OkonomiConfig(
        minimumTilsagnPeriodeStart = Tiltakskode.entries.associateWith { LocalDate.of(2025, 1, 1) },
    ),
)
