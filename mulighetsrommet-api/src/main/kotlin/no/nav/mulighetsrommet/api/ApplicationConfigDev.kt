package no.nav.mulighetsrommet.api

import no.nav.common.kafka.util.KafkaPropertiesPreset
import no.nav.mulighetsrommet.api.avtale.task.NotifySluttdatoForAvtalerNarmerSeg
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.gjennomforing.task.NotifySluttdatoForGjennomforingerNarmerSeg
import no.nav.mulighetsrommet.api.gjennomforing.task.UpdateApentForPamelding
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.navansatt.task.SynchronizeNavAnsatte
import no.nav.mulighetsrommet.api.navenhet.task.SynchronizeNorgEnheter
import no.nav.mulighetsrommet.api.tasks.GenerateValidationReport
import no.nav.mulighetsrommet.api.tasks.NotifyFailedKafkaEvents
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
        producerProperties = KafkaPropertiesPreset.aivenByteProducerProperties("mulighetsrommet-api-kafka-producer.v1"),
        consumerPreset = KafkaPropertiesPreset.aivenDefaultConsumerProperties("mulighetsrommet-api-kafka-consumer.v1"),
        clients = KafkaClients(
            amtDeltakerV1 = KafkaTopicConsumer.Config(
                id = "amt-deltaker",
                topic = "amt.deltaker-v1",
                consumerGroupId = "mulighetsrommet-api.deltaker.v2",
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
        roles = setOf(
            AdGruppeNavAnsattRolleMapping(
                adGruppeId = "639e2806-4cc2-484c-a72a-51b4308c52a1".toUUID(),
                rolle = Rolle.TEAM_MULIGHETSROMMET,
            ),
            AdGruppeNavAnsattRolleMapping(
                adGruppeId = "639e2806-4cc2-484c-a72a-51b4308c52a1".toUUID(),
                rolle = Rolle.TILTAKADMINISTRASJON_GENERELL,
            ),
            AdGruppeNavAnsattRolleMapping(
                adGruppeId = "52bb9196-b071-4cc7-9472-be4942d33c4b".toUUID(),
                rolle = Rolle.TILTAKADMINISTRASJON_GENERELL,
            ),
            AdGruppeNavAnsattRolleMapping(
                adGruppeId = "279039a0-39fd-4860-afdd-a1a2ccaa6323".toUUID(),
                rolle = Rolle.TILTAKSGJENNOMFORINGER_SKRIV,
            ),
            AdGruppeNavAnsattRolleMapping(
                adGruppeId = "48026f54-6259-4c35-a148-bc4257bcaf03".toUUID(),
                rolle = Rolle.AVTALER_SKRIV,
            ),
            AdGruppeNavAnsattRolleMapping(
                adGruppeId = "d9f317a1-2444-4fcd-b696-df8dbd6cc942".toUUID(),
                rolle = Rolle.TILTAKADMINISTRASJON_ENDRINGSMELDING,
            ),
            AdGruppeNavAnsattRolleMapping(
                adGruppeId = "7b1d209a-f6c1-4c6e-84f2-02a1bb4c92ba".toUUID(),
                rolle = Rolle.KONTAKTPERSON,
            ),

            AdGruppeNavAnsattRolleMapping(
                adGruppeId = "b00ba197-c90a-4ff9-966e-6c9cf1c882bf".toUUID(),
                rolle = Rolle.SAKSBEHANDLER_OKONOMI,
                kommentar = "TODO: Legacy ad-gruppe, skal slettes",
            ),
            AdGruppeNavAnsattRolleMapping(
                adGruppeId = "d776c0f9-9c8a-4299-8d34-aa563925b00b".toUUID(),
                rolle = Rolle.SAKSBEHANDLER_OKONOMI,
                kommentar = "0000-CA-Tiltaksadministrasjon_saksbehandler-okonomi",
            ),

            AdGruppeNavAnsattRolleMapping(
                adGruppeId = "b00ba197-c90a-4ff9-966e-6c9cf1c882bf".toUUID(),
                rolle = Rolle.BESLUTTER_TILSAGN,
                kommentar = "TODO: Legacy ad-gruppe, skal slettes",
            ),
            AdGruppeNavAnsattRolleMapping(
                adGruppeId = "f2d4a628-c17d-4ae0-b720-0abf9add8c30".toUUID(),
                rolle = Rolle.BESLUTTER_TILSAGN,
                kommentar = "0000-CA-Tiltaksadministrasjon_beslutter-tilsagn",
            ),
            AdGruppeNavAnsattRolleMapping(
                adGruppeId = "059b2db3-e38d-4482-bf2f-21d4a226aa94".toUUID(),
                rolle = Rolle.BESLUTTER_TILSAGN,
                kommentar = "0300-CA-Tiltaksadministrasjon_beslutter-tilsagn",
            ),
            AdGruppeNavAnsattRolleMapping(
                adGruppeId = "3e6fe3c5-58e2-466e-9117-adecb4c1ee74".toUUID(),
                rolle = Rolle.BESLUTTER_TILSAGN,
                kommentar = "0400-CA-Tiltaksadministrasjon_beslutter-tilsagn",
            ),

            AdGruppeNavAnsattRolleMapping(
                adGruppeId = "b00ba197-c90a-4ff9-966e-6c9cf1c882bf".toUUID(),
                rolle = Rolle.ATTESTANT_UTBETALING,
                kommentar = "TODO: Legacy ad-gruppe, skal slettes",
            ),
            AdGruppeNavAnsattRolleMapping(
                adGruppeId = "a9fb2838-fd9f-4bbd-aa41-2cabc83b26ac".toUUID(),
                rolle = Rolle.ATTESTANT_UTBETALING,
                kommentar = "0000-CA-Tiltaksadministrasjon_attestant-utbetaling",
            ),
            AdGruppeNavAnsattRolleMapping(
                adGruppeId = "88c9cffb-bb4f-4e9d-9af3-cf66ac11f156".toUUID(),
                rolle = Rolle.ATTESTANT_UTBETALING,
                kommentar = "0300-CA-Tiltaksadministrasjon_attestant-utbetaling",
            ),
            AdGruppeNavAnsattRolleMapping(
                adGruppeId = "e79ba9ba-efd9-456b-8567-dadbe65f8b24".toUUID(),
                rolle = Rolle.BESLUTTER_TILSAGN,
                kommentar = "0400-CA-Tiltaksadministrasjon_attestant-utbetaling",
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
        url = "https://sokos-kontoregister-q1.dev-fss-pub.nais.io",
        scope = "api://dev-fss.okonomi.sokos-kontoregister/.default",
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
    clamav = HttpClientConfig(
        url = "http://clamav.nais-system.svc.nais.local/scan"
    )
)
