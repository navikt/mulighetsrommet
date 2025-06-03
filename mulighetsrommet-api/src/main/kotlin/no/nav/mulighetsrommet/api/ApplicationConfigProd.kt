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
import no.nav.mulighetsrommet.api.utbetaling.task.GenerateUtbetaling
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.model.NavEnhetNummer
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
        clients = KafkaClients(::aivenDefaultConsumerProperties),
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
                entraGroupId = "33053061-86da-4d6b-9372-33238fabd25f".toUUID(),
                kommentar = "0000-GA-TILTAK-tiltaksgjennomforinger_skriv",
                rolle = Rolle.TILTAKSGJENNOMFORINGER_SKRIV,
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "46ba8787-eb24-4f7b-830f-4c5e9256de65".toUUID(),
                kommentar = "0000-GA-TILTAK-avtaler_skriv",
                rolle = Rolle.AVTALER_SKRIV,
            ),

            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "a54fd054-4047-46a6-be7c-f1b69f346be8".toUUID(),
                kommentar = "0000-CA-Tiltaksadministrasjon_saksbehandler-økonomi",
                rolle = Rolle.SAKSBEHANDLER_OKONOMI,
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "8eb13232-5a99-4e03-8f43-059dbd368ece".toUUID(),
                kommentar = "0200-CA-Tiltaksadministrasjon_attestant-utbetaling",
                rolle = Rolle.ATTESTANT_UTBETALING,
                enheter = setOf(NavEnhetNummer("0200")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "7db540e4-f2a7-4942-ab5f-dc54203a088f".toUUID(),
                kommentar = "0200-CA-Tiltaksadministrasjon_beslutter-tilsagn",
                rolle = Rolle.BESLUTTER_TILSAGN,
                enheter = setOf(NavEnhetNummer("0200")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "af433bab-7bf6-4ed2-81bc-e9675fb763ef".toUUID(),
                kommentar = "0300-CA-Tiltaksadministrasjon_attestant-utbetaling",
                rolle = Rolle.ATTESTANT_UTBETALING,
                enheter = setOf(NavEnhetNummer("0300")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "fd2a78f7-934d-4abf-95af-a6ee967cbe5d".toUUID(),
                kommentar = "0300-CA-Tiltaksadministrasjon_beslutter-tilsagn",
                rolle = Rolle.BESLUTTER_TILSAGN,
                enheter = setOf(NavEnhetNummer("0300")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "edc8a889-ffc2-4813-921b-8eea4fe0fd76".toUUID(),
                kommentar = "0400-CA-Tiltaksadministrasjon_attestant-utbetaling",
                rolle = Rolle.ATTESTANT_UTBETALING,
                enheter = setOf(NavEnhetNummer("0400")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "77b9322c-9ac7-479a-9403-ead8706b6d66".toUUID(),
                kommentar = "0400-CA-Tiltaksadministrasjon_beslutter-tilsagn",
                rolle = Rolle.BESLUTTER_TILSAGN,
                enheter = setOf(NavEnhetNummer("0400")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "5d5bc9ae-fe49-43dc-8255-cabac857542b".toUUID(),
                kommentar = "0600-CA-Tiltaksadministrasjon_attestant-utbetaling",
                rolle = Rolle.ATTESTANT_UTBETALING,
                enheter = setOf(NavEnhetNummer("0600")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "28d01506-93e1-4390-9b8c-7885c876fc10".toUUID(),
                kommentar = "0600-CA-Tiltaksadministrasjon_beslutter-tilsagn",
                rolle = Rolle.BESLUTTER_TILSAGN,
                enheter = setOf(NavEnhetNummer("0600")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "ba86368e-d776-4909-89e5-acb5b7932428".toUUID(),
                kommentar = "0800-CA-Tiltaksadministrasjon_attestant-utbetaling",
                rolle = Rolle.ATTESTANT_UTBETALING,
                enheter = setOf(NavEnhetNummer("0800")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "43c8bf56-5c32-41f7-8d79-017d83335dc3".toUUID(),
                kommentar = "0800-CA-Tiltaksadministrasjon_beslutter-tilsagn",
                rolle = Rolle.BESLUTTER_TILSAGN,
                enheter = setOf(NavEnhetNummer("0800")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "c536c283-1407-4043-967a-d4fbf5b44bbf".toUUID(),
                kommentar = "1000-CA-Tiltaksadministrasjon_attestant-utbetaling",
                rolle = Rolle.ATTESTANT_UTBETALING,
                enheter = setOf(NavEnhetNummer("1000")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "76ba924c-e029-4e69-a571-e3acc35a4d8b".toUUID(),
                kommentar = "1000-CA-Tiltaksadministrasjon_beslutter-tilsagn",
                rolle = Rolle.BESLUTTER_TILSAGN,
                enheter = setOf(NavEnhetNummer("1000")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "368e20e4-12d4-4026-b852-275f356301be".toUUID(),
                kommentar = "1100-CA-Tiltaksadministrasjon_attestant-utbetaling",
                rolle = Rolle.ATTESTANT_UTBETALING,
                enheter = setOf(NavEnhetNummer("1100")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "7bda4d14-01f1-49c0-85f9-c75308188331".toUUID(),
                kommentar = "1100-CA-Tiltaksadministrasjon_beslutter-tilsagn",
                rolle = Rolle.BESLUTTER_TILSAGN,
                enheter = setOf(NavEnhetNummer("1100")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "1d2b9d34-ead6-428d-96de-6e677a77afb0".toUUID(),
                kommentar = "1200-CA-Tiltaksadministrasjon_attestant-utbetaling",
                rolle = Rolle.ATTESTANT_UTBETALING,
                enheter = setOf(NavEnhetNummer("1200")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "c9e0307d-355a-47ef-8776-132746ebbb15".toUUID(),
                kommentar = "1200-CA-Tiltaksadministrasjon_beslutter-tilsagn",
                rolle = Rolle.BESLUTTER_TILSAGN,
                enheter = setOf(NavEnhetNummer("1200")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "b9f39ac5-6b6f-47a4-a23a-2218c46ee18d".toUUID(),
                kommentar = "1500-CA-Tiltaksadministrasjon_attestant-utbetaling",
                rolle = Rolle.ATTESTANT_UTBETALING,
                enheter = setOf(NavEnhetNummer("1500")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "949db0d2-68ff-413f-87ab-39cd49ec305b".toUUID(),
                kommentar = "1500-CA-Tiltaksadministrasjon_beslutter-tilsagn",
                rolle = Rolle.BESLUTTER_TILSAGN,
                enheter = setOf(NavEnhetNummer("1500")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "7c11c112-f82c-4a4b-86a0-3769335599af".toUUID(),
                kommentar = "1800-CA-Tiltaksadministrasjon_attestant-utbetaling",
                rolle = Rolle.ATTESTANT_UTBETALING,
                enheter = setOf(NavEnhetNummer("1800")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "f576ab19-837e-4654-a252-e6ca0b62ba03".toUUID(),
                kommentar = "1800-CA-Tiltaksadministrasjon_beslutter-tilsagn",
                rolle = Rolle.BESLUTTER_TILSAGN,
                enheter = setOf(NavEnhetNummer("1800")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "37706a74-6b78-4719-9f63-11a2f8bfd37c".toUUID(),
                kommentar = "1900-CA-Tiltaksadministrasjon_attestant-utbetaling",
                rolle = Rolle.ATTESTANT_UTBETALING,
                enheter = setOf(NavEnhetNummer("1900")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "c2f30e37-c74c-4660-a632-37e8e8e80329".toUUID(),
                kommentar = "1900-CA-Tiltaksadministrasjon_beslutter-tilsagn",
                rolle = Rolle.BESLUTTER_TILSAGN,
                enheter = setOf(NavEnhetNummer("1900")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "92b61aff-7958-4fec-b0c2-604530f45ed5".toUUID(),
                kommentar = "5700-CA-Tiltaksadministrasjon_attestant-utbetaling",
                rolle = Rolle.ATTESTANT_UTBETALING,
                enheter = setOf(NavEnhetNummer("5700")),
            ),
            EntraGroupNavAnsattRolleMapping(
                entraGroupId = "93b27697-0d6d-4f1f-bf63-2f2134bf2cca".toUUID(),
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
        url = "http://clamav.nais-system",
    ),
)
