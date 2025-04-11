package no.nav.mulighetsrommet.api

import no.nav.common.kafka.util.KafkaPropertiesPreset
import no.nav.mulighetsrommet.api.avtale.task.NotifySluttdatoForAvtalerNarmerSeg
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.gjennomforing.task.NotifySluttdatoForGjennomforingerNarmerSeg
import no.nav.mulighetsrommet.api.gjennomforing.task.UpdateApentForPamelding
import no.nav.mulighetsrommet.api.navansatt.NavAnsattSyncService
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.navansatt.task.SynchronizeNavAnsatte
import no.nav.mulighetsrommet.api.navenhet.task.SynchronizeNorgEnheter
import no.nav.mulighetsrommet.api.tasks.GenerateValidationReport
import no.nav.mulighetsrommet.api.tasks.NotifyFailedKafkaEvents
import no.nav.mulighetsrommet.api.utbetaling.task.GenerateUtbetaling
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.unleash.UnleashService
import no.nav.mulighetsrommet.utdanning.task.SynchronizeUtdanninger
import no.nav.mulighetsrommet.utils.toUUID
import java.time.LocalDate

private val teamMulighetsrommetAdGruppeId = "debefa6e-1865-446d-b22b-9579fc735de3".toUUID()
private val tiltaksadministrasjonAdGruppeId = "2cf8d881-c2da-47b5-b409-fa088440a629".toUUID()
private val kontaktpersonAdGruppeId = "0fdd133a-f47f-4b95-9a5e-f3a5ec87a472".toUUID()

val ApplicationConfigProd = AppConfig(
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
        clients = KafkaClients(),
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
                adGruppeId = teamMulighetsrommetAdGruppeId,
                rolle = Rolle.TEAM_MULIGHETSROMMET,
                kommentar = "Team Mulighetsrommet",
            ),
            AdGruppeNavAnsattRolleMapping(
                adGruppeId = teamMulighetsrommetAdGruppeId,
                rolle = Rolle.TILTAKADMINISTRASJON_GENERELL,
                kommentar = "Team Mulighetsrommet",
            ),
            AdGruppeNavAnsattRolleMapping(
                adGruppeId = tiltaksadministrasjonAdGruppeId,
                rolle = Rolle.TILTAKADMINISTRASJON_GENERELL,
                kommentar = "0000-GA-TILTAK-ANSVARLIG",
            ),
            AdGruppeNavAnsattRolleMapping(
                adGruppeId = "33053061-86da-4d6b-9372-33238fabd25f".toUUID(),
                rolle = Rolle.TILTAKSGJENNOMFORINGER_SKRIV,
                kommentar = "0000-GA-TILTAK-tiltaksgjennomforinger_skriv",
            ),
            AdGruppeNavAnsattRolleMapping(
                adGruppeId = "46ba8787-eb24-4f7b-830f-4c5e9256de65".toUUID(),
                rolle = Rolle.AVTALER_SKRIV,
                kommentar = "0000-GA-TILTAK-avtaler_skriv",
            ),
            AdGruppeNavAnsattRolleMapping(
                adGruppeId = "4e4bfc3e-58c5-4f1c-879b-df1a86016de9".toUUID(),
                rolle = Rolle.TILTAKADMINISTRASJON_ENDRINGSMELDING,
                kommentar = "0000-GA-TILTAK-ENDRINGSMELDING",
            ),
            AdGruppeNavAnsattRolleMapping(
                adGruppeId = kontaktpersonAdGruppeId,
                rolle = Rolle.KONTAKTPERSON,
                kommentar = "(GRP) mr-nav_kontaktperson",
            ),
            AdGruppeNavAnsattRolleMapping(
                adGruppeId = "6a1f1984-0fe3-4a0e-ac6e-19225b604a52".toUUID(),
                rolle = Rolle.SAKSBEHANDLER_OKONOMI,
                kommentar = "0000-GA-TILTAK-okonomi_beslutter",
            ),
            AdGruppeNavAnsattRolleMapping(
                adGruppeId = "6a1f1984-0fe3-4a0e-ac6e-19225b604a52".toUUID(),
                rolle = Rolle.BESLUTTER_TILSAGN,
                kommentar = "TODO: slett denne",
            ),
            AdGruppeNavAnsattRolleMapping(
                adGruppeId = "6a1f1984-0fe3-4a0e-ac6e-19225b604a52".toUUID(),
                rolle = Rolle.ATTESTANT_UTBETALING,
                kommentar = "TODO: slett denne",
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
        scope = "api://prod-gcp.poao.veilarboppfolging/.default",
    ),
    veilarbvedtaksstotteConfig = AuthenticatedHttpClientConfig(
        url = "http://veilarbvedtaksstotte.obo/veilarbvedtaksstotte/api",
        scope = "api://prod-gcp.obo.veilarbvedtaksstotte/.default",
    ),
    veilarbdialogConfig = AuthenticatedHttpClientConfig(
        url = "http://veilarbdialog.dab/veilarbdialog/api",
        scope = "api://prod-gcp.dab.veilarbdialog/.default",
    ),
    amtDeltakerConfig = AuthenticatedHttpClientConfig(
        url = "http://amt-deltaker.amt",
        scope = "api://prod-gcp.amt.amt-deltaker/.default",
    ),
    poaoTilgang = AuthenticatedHttpClientConfig(
        url = "http://poao-tilgang.poao",
        scope = "api://prod-gcp.poao.poao-tilgang/.default",
    ),
    arenaAdapter = AuthenticatedHttpClientConfig(
        url = "http://mulighetsrommet-arena-adapter",
        scope = "api://prod-gcp.team-mulighetsrommet.mulighetsrommet-arena-adapter/.default",
    ),
    tiltakshistorikk = AuthenticatedHttpClientConfig(
        url = "http://tiltakshistorikk",
        scope = "api://prod-gcp.team-mulighetsrommet.tiltakshistorikk/.default",
    ),
    pdfgen = HttpClientConfig(url = "http://pdfgen"),
    msGraphConfig = AuthenticatedHttpClientConfig(
        url = "https://graph.microsoft.com",
        scope = "https://graph.microsoft.com/.default",
    ),
    isoppfolgingstilfelleConfig = AuthenticatedHttpClientConfig(
        url = "http://isoppfolgingstilfelle.teamsykefravr",
        scope = "api://prod-gcp.teamsykefravr.isoppfolgingstilfelle/.default",
    ),
    norg2 = HttpClientConfig(url = "http://norg2.org"),
    pamOntologi = AuthenticatedHttpClientConfig(
        url = "http://pam-ontologi.teampam",
        scope = "api://prod-gcp.teampam.pam-ontologi/.default",
    ),
    unleash = UnleashService.Config(
        appName = System.getenv("NAIS_APP_NAME"),
        url = System.getenv("UNLEASH_SERVER_API_URL"),
        token = System.getenv("UNLEASH_SERVER_API_TOKEN"),
        instanceId = System.getenv("NAIS_CLIENT_ID"),
        environment = "production",
    ),
    axsys = AuthenticatedHttpClientConfig(
        url = "https://axsys.prod-fss-pub.nais.io",
        scope = "api://prod-fss.org.axsys/.default",
    ),
    pdl = AuthenticatedHttpClientConfig(
        url = "https://pdl-api.prod-fss-pub.nais.io",
        scope = "api://prod-fss.pdl.pdl-api/.default",
    ),
    utdanning = HttpClientConfig(
        url = "https://api.utdanning.no",
    ),
    altinn = AuthenticatedHttpClientConfig(
        url = "https://platform.altinn.no",
        scope = System.getenv("MASKINPORTEN_SCOPES"),
    ),
    dokark = AuthenticatedHttpClientConfig(
        url = "https://dokarkiv-q2.dev-fss-pub.nais.io",
        scope = "api://dev-fss.teamdokumenthandtering.dokarkiv/.default",
    ),
    kontoregisterOrganisasjon = AuthenticatedHttpClientConfig(
        url = "https://sokos-kontoregister.prod-fss-pub.nais.io",
        scope = "api://prod-fss.okonomi.sokos-kontoregister/.default",
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
            bucketName = "mulighetsrommet-api-uploads-prod",
        ),
        updateApentForPamelding = UpdateApentForPamelding.Config(
            disabled = false,
            cronPattern = "0 55 23 * * *",
        ),
        generateUtbetaling = GenerateUtbetaling.Config(
            disabled = true,
            cronPattern = "0 0 5 7 * *",
        ),
    ),
    slack = SlackConfig(
        token = System.getenv("SLACK_TOKEN"),
        channel = "#team-valp-monitoring",
        enable = true,
    ),
    okonomi = OkonomiConfig(
        minimumTilsagnPeriodeStart = mapOf(
            // Forhåndsgodkjente tiltak
            Tiltakskode.ARBEIDSFORBEREDENDE_TRENING to LocalDate.of(2025, 7, 1),
            Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET to LocalDate.of(2025, 7, 1),
        ),
    ),
    clamav = HttpClientConfig(
        url = "http://clamav.nais-system.svc.nais.local/scan",
    ),
)
