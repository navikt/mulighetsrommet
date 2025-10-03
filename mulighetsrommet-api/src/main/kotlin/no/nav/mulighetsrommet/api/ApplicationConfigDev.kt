package no.nav.mulighetsrommet.api

import no.nav.common.kafka.util.KafkaPropertiesPreset
import no.nav.common.kafka.util.KafkaPropertiesPreset.aivenDefaultConsumerProperties
import no.nav.mulighetsrommet.api.avtale.task.NotifySluttdatoForAvtalerNarmerSeg
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.gjennomforing.task.NotifySluttdatoForGjennomforingerNarmerSeg
import no.nav.mulighetsrommet.api.gjennomforing.task.UpdateApentForPamelding
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.navansatt.service.NavAnsattSyncService
import no.nav.mulighetsrommet.api.navansatt.task.SynchronizeNavAnsatte
import no.nav.mulighetsrommet.api.navenhet.task.SynchronizeNorgEnheter
import no.nav.mulighetsrommet.api.tasks.GenerateValidationReport
import no.nav.mulighetsrommet.api.utbetaling.task.BeregnUtbetaling
import no.nav.mulighetsrommet.api.utbetaling.task.GenerateUtbetaling
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.metrics.Metrics
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.tokenprovider.TexasClient
import no.nav.mulighetsrommet.unleash.UnleashService
import no.nav.mulighetsrommet.utdanning.task.SynchronizeUtdanninger
import no.nav.mulighetsrommet.utils.toUUID
import java.time.LocalDate

private val teamMulighetsrommetAdGruppeId = "639e2806-4cc2-484c-a72a-51b4308c52a1".toUUID()
private val tiltaksadministrasjonAdGruppeId = "52bb9196-b071-4cc7-9472-be4942d33c4b".toUUID()
private val kontaktpersonAdGruppeId = "7b1d209a-f6c1-4c6e-84f2-02a1bb4c92ba".toUUID()

val ApplicationConfigDev = AppConfig(
    database = DatabaseConfig(
        jdbcUrl = System.getenv("DB_JDBC_URL"),
        maximumPoolSize = 10,
        micrometerRegistry = Metrics.micrometerRegistry,
    ),
    flyway = FlywayMigrationManager.MigrationConfig(
        strategy = FlywayMigrationManager.InitializationStrategy.Migrate,
    ),
    kafka = KafkaConfig(
        producerProperties = KafkaPropertiesPreset.aivenByteProducerProperties("mulighetsrommet-api-kafka-producer.v1"),
        clients = KafkaClients(::aivenDefaultConsumerProperties) {
            amtDeltakerV1 = KafkaTopicConsumer.Config(
                id = "amt-deltaker",
                topic = "amt.deltaker-v1",
                consumerProperties = aivenDefaultConsumerProperties("mulighetsrommet-api.deltaker.v2"),
            )
        },
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
        texas = TexasClient.Config(
            tokenEndpoint = System.getenv("NAIS_TOKEN_ENDPOINT"),
            tokenExchangeEndpoint = System.getenv("NAIS_TOKEN_EXCHANGE_ENDPOINT"),
            tokenIntrospectionEndpoint = System.getenv("NAIS_TOKEN_INTROSPECTION_ENDPOINT"),
        ),
        roles = setOf(
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = teamMulighetsrommetAdGruppeId,
                rolle = Rolle.TEAM_MULIGHETSROMMET,
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = teamMulighetsrommetAdGruppeId,
                rolle = Rolle.TILTAKADMINISTRASJON_GENERELL,
            ),

            EntraGroupNavAnsattRolleMapping(
                entraGroupId = tiltaksadministrasjonAdGruppeId,
                kommentar = "0000-GA-TILTAK-ANSVARLIG",
                rolle = Rolle.TILTAKADMINISTRASJON_GENERELL,
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = kontaktpersonAdGruppeId,
                kommentar = "0000-CA-Tiltaksadministrasjon_kontaktperson",
                rolle = Rolle.KONTAKTPERSON,
            ),

            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "48026f54-6259-4c35-a148-bc4257bcaf03".toUUID(),
                kommentar = "0000-CA-Tiltaksadministrasjon_avtale-skriv",
                rolle = Rolle.AVTALER_SKRIV,
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "279039a0-39fd-4860-afdd-a1a2ccaa6323".toUUID(),
                kommentar = "0000-CA-Tiltaksadministrasjon_gjennomforing-skriv",
                rolle = Rolle.TILTAKSGJENNOMFORINGER_SKRIV,
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "cbf319db-ac5a-4cb2-bfc4-7f813f97cca6".toUUID(),
                kommentar = "0000-CA-Tiltaksadministrasjon_oppfølger-gjennomføring",
                rolle = Rolle.OPPFOLGER_GJENNOMFORING,
            ),

            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "b78d7080-aed9-490b-bd38-2ef3a283cd1a".toUUID(),
                kommentar = "0000-CA-Tiltaksadministrasjon_lesetilgang-økonomi",
                rolle = Rolle.OKONOMI_LES,
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "d776c0f9-9c8a-4299-8d34-aa563925b00b".toUUID(),
                kommentar = "0000-CA-Tiltaksadministrasjon_saksbehandler-økonomi",
                rolle = Rolle.SAKSBEHANDLER_OKONOMI,
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "a9fb2838-fd9f-4bbd-aa41-2cabc83b26ac".toUUID(),
                kommentar = "0000-CA-Tiltaksadministrasjon_attestant-utbetaling",
                rolle = Rolle.ATTESTANT_UTBETALING,
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "f2d4a628-c17d-4ae0-b720-0abf9add8c30".toUUID(),
                kommentar = "0000-CA-Tiltaksadministrasjon_beslutter-tilsagn",
                rolle = Rolle.BESLUTTER_TILSAGN,
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "1edfb41e-8689-4c97-8e65-e3450cb06b43".toUUID(),
                kommentar = "0200-CA-Tiltaksadministrasjon_attestant-utbetaling",
                rolle = Rolle.ATTESTANT_UTBETALING,
                enheter = setOf(NavEnhetNummer("0200")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "33b600e0-0b88-4097-9052-525f0eb10191".toUUID(),
                kommentar = "0200-CA-Tiltaksadministrasjon_beslutter-tilsagn",
                rolle = Rolle.BESLUTTER_TILSAGN,
                enheter = setOf(NavEnhetNummer("0200")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "88c9cffb-bb4f-4e9d-9af3-cf66ac11f156".toUUID(),
                kommentar = "0300-CA-Tiltaksadministrasjon_attestant-utbetaling",
                rolle = Rolle.ATTESTANT_UTBETALING,
                enheter = setOf(NavEnhetNummer("0300")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "059b2db3-e38d-4482-bf2f-21d4a226aa94".toUUID(),
                kommentar = "0300-CA-Tiltaksadministrasjon_beslutter-tilsagn",
                rolle = Rolle.BESLUTTER_TILSAGN,
                enheter = setOf(NavEnhetNummer("0300")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "e79ba9ba-efd9-456b-8567-dadbe65f8b24".toUUID(),
                kommentar = "0400-CA-Tiltaksadministrasjon_attestant-utbetaling",
                rolle = Rolle.ATTESTANT_UTBETALING,
                enheter = setOf(NavEnhetNummer("0400")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "3e6fe3c5-58e2-466e-9117-adecb4c1ee74".toUUID(),
                kommentar = "0400-CA-Tiltaksadministrasjon_beslutter-tilsagn",
                rolle = Rolle.BESLUTTER_TILSAGN,
                enheter = setOf(NavEnhetNummer("0400")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "ddb6d18c-cadb-4cbc-b0d2-58ad535ca16c".toUUID(),
                kommentar = "0600-CA-Tiltaksadministrasjon_attestant-utbetaling",
                rolle = Rolle.ATTESTANT_UTBETALING,
                enheter = setOf(NavEnhetNummer("0600")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "202216c0-bd70-4466-a902-f1809d588a79".toUUID(),
                kommentar = "0600-CA-Tiltaksadministrasjon_beslutter-tilsagn",
                rolle = Rolle.BESLUTTER_TILSAGN,
                enheter = setOf(NavEnhetNummer("0600")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "a8efb768-31c2-4ac2-acda-d7dafbaa5e58".toUUID(),
                kommentar = "0800-CA-Tiltaksadministrasjon_attestant-utbetaling",
                rolle = Rolle.ATTESTANT_UTBETALING,
                enheter = setOf(NavEnhetNummer("0800")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "671c4e78-1b39-42fc-9182-b60508166365".toUUID(),
                kommentar = "0800-CA-Tiltaksadministrasjon_beslutter-tilsagn",
                rolle = Rolle.BESLUTTER_TILSAGN,
                enheter = setOf(NavEnhetNummer("0800")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "2eeb8676-11b3-41f0-91da-a68a9fd51ba4".toUUID(),
                kommentar = "1000-CA-Tiltaksadministrasjon_attestant-utbetaling",
                rolle = Rolle.ATTESTANT_UTBETALING,
                enheter = setOf(NavEnhetNummer("1000")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "0f2013ef-1b46-4701-8d7b-033fbfb73efc".toUUID(),
                kommentar = "1000-CA-Tiltaksadministrasjon_beslutter-tilsagn",
                rolle = Rolle.BESLUTTER_TILSAGN,
                enheter = setOf(NavEnhetNummer("1000")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "100d863c-b686-4c36-bf46-a37167552377".toUUID(),
                kommentar = "1100-CA-Tiltaksadministrasjon_attestant-utbetaling",
                rolle = Rolle.ATTESTANT_UTBETALING,
                enheter = setOf(NavEnhetNummer("1100")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "881b995e-1b02-4729-8a27-ba4b378df6d6".toUUID(),
                kommentar = "1100-CA-Tiltaksadministrasjon_beslutter-tilsagn",
                rolle = Rolle.BESLUTTER_TILSAGN,
                enheter = setOf(NavEnhetNummer("1100")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "dcbccfab-e6df-41aa-b57c-2775320e8955".toUUID(),
                kommentar = "1200-CA-Tiltaksadministrasjon_attestant-utbetaling",
                rolle = Rolle.ATTESTANT_UTBETALING,
                enheter = setOf(NavEnhetNummer("1200")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "20614177-ec1a-47bf-a89c-cd0d99e1e790".toUUID(),
                kommentar = "1200-CA-Tiltaksadministrasjon_beslutter-tilsagn",
                rolle = Rolle.BESLUTTER_TILSAGN,
                enheter = setOf(NavEnhetNummer("1200")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "8e4e5710-5321-438b-9aa0-96a1e32eab71".toUUID(),
                kommentar = "1500-CA-Tiltaksadministrasjon_attestant-utbetaling",
                rolle = Rolle.ATTESTANT_UTBETALING,
                enheter = setOf(NavEnhetNummer("1500")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "ad23027c-5185-4011-8b50-1a3799f4a847".toUUID(),
                kommentar = "1500-CA-Tiltaksadministrasjon_beslutter-tilsagn",
                rolle = Rolle.BESLUTTER_TILSAGN,
                enheter = setOf(NavEnhetNummer("1500")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "6ecef1a0-cb8e-45d4-afb5-115116d0aab4".toUUID(),
                kommentar = "1800-CA-Tiltaksadministrasjon_attestant-utbetaling",
                rolle = Rolle.ATTESTANT_UTBETALING,
                enheter = setOf(NavEnhetNummer("1800")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "a2c0ba66-c564-472b-8e39-b8fc74ed7f48".toUUID(),
                kommentar = "1800-CA-Tiltaksadministrasjon_beslutter-tilsagn",
                rolle = Rolle.BESLUTTER_TILSAGN,
                enheter = setOf(NavEnhetNummer("1800")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "95dfba28-62da-49fb-ae3c-237573c6eaeb".toUUID(),
                kommentar = "1900-CA-Tiltaksadministrasjon_attestant-utbetaling",
                rolle = Rolle.ATTESTANT_UTBETALING,
                enheter = setOf(NavEnhetNummer("1900")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "35c4d0f9-a7ac-4588-8762-5913cb787058".toUUID(),
                kommentar = "1900-CA-Tiltaksadministrasjon_beslutter-tilsagn",
                rolle = Rolle.BESLUTTER_TILSAGN,
                enheter = setOf(NavEnhetNummer("1900")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "f57989e2-410e-45dd-9285-32c3bacc94d4".toUUID(),
                kommentar = "5700-CA-Tiltaksadministrasjon_attestant-utbetaling",
                rolle = Rolle.ATTESTANT_UTBETALING,
                enheter = setOf(NavEnhetNummer("5700")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "34902073-90be-4025-9bb0-da6b71b293ff".toUUID(),
                kommentar = "5700-CA-Tiltaksadministrasjon_beslutter-tilsagn",
                rolle = Rolle.BESLUTTER_TILSAGN,
                enheter = setOf(NavEnhetNummer("5700")),
            ),
        ),
    ),
    navAnsattSync = NavAnsattSyncService.Config(
        ansattGroupsToSync = setOf(
            teamMulighetsrommetAdGruppeId,
            tiltaksadministrasjonAdGruppeId,
            kontaktpersonAdGruppeId,
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
        beregnUtbetaling = BeregnUtbetaling.Config(
            bucketName = "mulighetsrommet-api-uploads-dev",
        ),
    ),
    slack = SlackConfig(
        token = System.getenv("SLACK_TOKEN"),
        channel = "#team-valp-monitorering-dev",
        enable = true,
    ),
    okonomi = OkonomiConfig(
        gyldigTilsagnPeriode = Tiltakskode.entries.associateWith { Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 1)) },
    ),
    clamav = HttpClientConfig(
        url = "http://clamav.nais-system",
    ),

)
