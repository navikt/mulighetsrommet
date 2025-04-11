package no.nav.mulighetsrommet.api

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import no.nav.mulighetsrommet.api.avtale.task.NotifySluttdatoForAvtalerNarmerSeg
import no.nav.mulighetsrommet.api.clients.pdl.GraphqlRequest
import no.nav.mulighetsrommet.api.clients.pdl.GraphqlRequest.Identer
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.gjennomforing.task.NotifySluttdatoForGjennomforingerNarmerSeg
import no.nav.mulighetsrommet.api.gjennomforing.task.UpdateApentForPamelding
import no.nav.mulighetsrommet.api.navansatt.NavAnsattSyncService
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.navansatt.task.SynchronizeNavAnsatte
import no.nav.mulighetsrommet.api.navenhet.task.SynchronizeNorgEnheter
import no.nav.mulighetsrommet.api.tasks.NotifyFailedKafkaEvents
import no.nav.mulighetsrommet.api.utbetaling.task.GenerateUtbetaling
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import no.nav.mulighetsrommet.tokenprovider.createMockRSAKey
import no.nav.mulighetsrommet.unleash.UnleashService
import no.nav.mulighetsrommet.utdanning.task.SynchronizeUtdanninger
import no.nav.mulighetsrommet.utils.toUUID
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.ByteArraySerializer
import java.time.LocalDate

val ApplicationConfigLocal = AppConfig(
    database = DatabaseConfig(
        jdbcUrl = "jdbc:postgresql://localhost:5442/mr-api?user=valp&password=valp",
        maximumPoolSize = 10,
    ),
    flyway = FlywayMigrationManager.MigrationConfig(
        strategy = FlywayMigrationManager.InitializationStrategy.RepairAndMigrate,
    ),
    kafka = KafkaConfig(
        producerProperties = KafkaPropertiesBuilder.producerBuilder()
            .withBaseProperties()
            .withProducerId("mulighetsrommet-api-kafka-producer.v1")
            .withBrokerUrl("localhost:29092")
            .withSerializers(ByteArraySerializer::class.java, ByteArraySerializer::class.java)
            .build(),
        consumerPreset = KafkaPropertiesBuilder.consumerBuilder()
            .withBaseProperties()
            .withConsumerGroupId("mulighetsrommet-api-kafka-consumer.v1")
            .withBrokerUrl("localhost:29092")
            .withDeserializers(ByteArrayDeserializer::class.java, ByteArrayDeserializer::class.java)
            .build(),
        clients = KafkaClients(),
    ),
    auth = AuthConfig(
        azure = AuthProvider(
            issuer = "http://localhost:8081/azure",
            jwksUri = "http://localhost:8081/azure/jwks",
            audience = "mulighetsrommet-api",
            tokenEndpointUrl = "http://localhost:8081/azure/token",
            privateJwk = createMockRSAKey("azure"),
        ),
        tokenx = AuthProvider(
            issuer = "http://localhost:8081/tokenx",
            jwksUri = "http://localhost:8081/tokenx/jwks",
            audience = "mulighetsrommet-api",
            tokenEndpointUrl = "http://localhost:8081/tokenx/token",
            privateJwk = createMockRSAKey("tokenx"),
        ),
        maskinporten = AuthProvider(
            issuer = "http://localhost:8081/maskinporten",
            jwksUri = "http://localhost:8081/maskinporten/jwks",
            audience = "mulighetsrommet-api",
            tokenEndpointUrl = "http://localhost:8081/maskinporten/token",
            privateJwk = createMockRSAKey("maskinporten"),
        ),
        roles = setOf(
            AdGruppeNavAnsattRolleMapping(
                "52bb9196-b071-4cc7-9472-be4942d33c4b".toUUID(),
                Rolle.TEAM_MULIGHETSROMMET,
            ),
            AdGruppeNavAnsattRolleMapping(
                "52bb9196-b071-4cc7-9472-be4942d33c4b".toUUID(),
                Rolle.TILTAKADMINISTRASJON_GENERELL,
            ),
            AdGruppeNavAnsattRolleMapping(
                "48026f54-6259-4c35-a148-bc4257bcaf03".toUUID(),
                Rolle.AVTALER_SKRIV,
            ),
            AdGruppeNavAnsattRolleMapping(
                "279039a0-39fd-4860-afdd-a1a2ccaa6323".toUUID(),
                Rolle.TILTAKSGJENNOMFORINGER_SKRIV,
            ),
            AdGruppeNavAnsattRolleMapping(
                "d9f317a1-2444-4fcd-b696-df8dbd6cc942".toUUID(),
                Rolle.TILTAKADMINISTRASJON_ENDRINGSMELDING,
            ),
            AdGruppeNavAnsattRolleMapping(
                "0fdd133a-f47f-4b95-9a5e-f3a5ec87a472".toUUID(),
                Rolle.KONTAKTPERSON,
            ),
            AdGruppeNavAnsattRolleMapping(
                "b00ba197-c90a-4ff9-966e-6c9cf1c882bf".toUUID(),
                Rolle.SAKSBEHANDLER_OKONOMI,
            ),
            AdGruppeNavAnsattRolleMapping(
                "b00ba197-c90a-4ff9-966e-6c9cf1c882bf".toUUID(),
                Rolle.BESLUTTER_TILSAGN,
            ),
            AdGruppeNavAnsattRolleMapping(
                "b00ba197-c90a-4ff9-966e-6c9cf1c882bf".toUUID(),
                Rolle.ATTESTANT_UTBETALING,
            ),
        ),
    ),
    navAnsattSync = NavAnsattSyncService.Config(setOf()),
    sanity = SanityClient.Config(
        dataset = "test",
        projectId = "xegcworx",
        token = System.getenv("SANITY_AUTH_TOKEN") ?: "",
        useCdn = false,
    ),
    slack = SlackConfig(
        token = System.getenv("SLACK_TOKEN") ?: "",
        channel = "#team-valp-monitoring",
        enable = false,
    ),
    unleash = UnleashService.Config(
        appName = "mulighetsrommet-api",
        url = "http://localhost:8090/unleash",
        token = "",
        instanceId = "mulighetsrommet-api",
        environment = "local",
    ),
    arenaAdapter = AuthenticatedHttpClientConfig(
        url = "http://0.0.0.0:8084",
        scope = "default",
    ),
    tiltakshistorikk = AuthenticatedHttpClientConfig(
        url = "http://0.0.0.0:8090",
        scope = "mr-tiltakshistorikk",
    ),
    pdfgen = HttpClientConfig(
        url = "http://localhost:8888",
    ),
    veilarbvedtaksstotteConfig = AuthenticatedHttpClientConfig(
        url = "http://localhost:8090/veilarbvedtaksstotte/api",
        scope = "default",
    ),
    veilarboppfolgingConfig = AuthenticatedHttpClientConfig(
        url = "http://localhost:8090/veilarboppfolging/api",
        scope = "default",
    ),
    veilarbdialogConfig = AuthenticatedHttpClientConfig(
        url = "http://localhost:8090/veilarbdialog/api",
        scope = "default",
    ),
    poaoTilgang = AuthenticatedHttpClientConfig(
        url = "http://localhost:8090/poao-tilgang",
        scope = "default",
    ),
    isoppfolgingstilfelleConfig = AuthenticatedHttpClientConfig(
        url = "http://localhost:8090/isoppfolgingstilfelle",
        scope = "default",
    ),
    msGraphConfig = AuthenticatedHttpClientConfig(
        url = "http://localhost:8090/ms-graph",
        scope = "default",
    ),
    amtDeltakerConfig = AuthenticatedHttpClientConfig(
        url = "http://localhost:8090/amt-deltaker",
        scope = "default",
    ),
    axsys = AuthenticatedHttpClientConfig(
        url = "http://localhost:8090/axsys",
        scope = "default",
    ),
    pdl = AuthenticatedHttpClientConfig(
        url = "http://localhost:8090/pdl",
        scope = "default",
        engine = MockEngine { request ->
            val parsedReq =
                JsonIgnoreUnknownKeys.decodeFromString<GraphqlRequest<JsonObject>>((request.body as TextContent).text)
            when {
                "identer" in parsedReq.variables.keys -> {
                    val identer = JsonIgnoreUnknownKeys.decodeFromJsonElement<Identer>(parsedReq.variables).identer
                    val hentPersonBolk = identer.joinToString(",\n") { ident ->
                        """
                          {
                            "ident": "${ident.value}",
                            "person": {
                              "navn": [
                                {
                                  "fornavn": "Ola",
                                  "mellomnavn": null,
                                  "etternavn": "Nordmann"
                                }
                              ],
                              "adressebeskyttelse": [
                              ],
                              "foedselsdato": [
                                {
                                  "foedselsaar": 1993,
                                  "foedselsdato": "1993-11-01"
                                }
                              ]
                            },
                            "code": "ok"
                          }
                        """.trimIndent()
                    }
                    val geografiskTilknytningBolk = identer.joinToString(",\n") { ident ->
                        """
                        {
                            "ident": "${ident.value}",
                            "geografiskTilknytning": {
                              "gtType": "BYDEL",
                              "gtLand": null,
                              "gtKommune": null,
                              "gtBydel": "030102"
                            },
                            "code": "ok"
                        }
                        """.trimIndent()
                    }
                    respond(
                        content = ByteReadChannel(
                            """
                            {
                              "data": {
                                "hentGeografiskTilknytningBolk": [$geografiskTilknytningBolk],
                                "hentPersonBolk": [$hentPersonBolk]
                              },
                              "errors": []
                            }
                            """.trimIndent(),
                        ),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

                else ->
                    respond(
                        content = ByteReadChannel(
                            """
                            {
                              "data": {
                                "hentGeografiskTilknytning": {
                                  "gtType": "BYDEL",
                                  "gtLand": null,
                                  "gtKommune": null,
                                  "gtBydel": "030102"
                                },
                                "hentPerson": {
                                  "navn": [
                                    {
                                      "fornavn": "Ola",
                                      "mellomnavn": null,
                                      "etternavn": "Nordmann"
                                    }
                                  ]
                                },
                                "hentIdenter": {
                                  "identer": [
                                    {
                                      "ident": "12118323058",
                                      "gruppe": "AKTORID",
                                      "historisk": false
                                    }
                                  ]
                                }
                              },
                              "errors": []
                            }
                            """.trimIndent(),
                        ),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
            }
        },
    ),
    pamOntologi = AuthenticatedHttpClientConfig(
        url = "http://localhost:8090",
        scope = "default",
    ),
    norg2 = HttpClientConfig(
        url = "http://localhost:8090/norg2",
    ),
    altinn = AuthenticatedHttpClientConfig(
        url = "http://localhost:8090/altinn",
        scope = "default",
    ),
    dokark = AuthenticatedHttpClientConfig(
        url = "http://localhost:8090/dokark",
        scope = "default",
    ),
    utdanning = HttpClientConfig(
        url = "https://api.utdanning.no",
    ),
    tasks = TaskConfig(
        synchronizeNorgEnheter = SynchronizeNorgEnheter.Config(
            disabled = true,
            delayOfMinutes = 360,
        ),
        synchronizeNavAnsatte = SynchronizeNavAnsatte.Config(
            disabled = true,
            cronPattern = "0 */1 * * * *",
        ),
        synchronizeUtdanninger = SynchronizeUtdanninger.Config(
            disabled = true,
            cronPattern = "0 */1 * * * *",
        ),
        notifySluttdatoForGjennomforingerNarmerSeg = NotifySluttdatoForGjennomforingerNarmerSeg.Config(
            disabled = true,
            cronPattern = "0 */1 * * * *",
        ),
        notifySluttdatoForAvtalerNarmerSeg = NotifySluttdatoForAvtalerNarmerSeg.Config(
            disabled = true,
            cronPattern = "0 */1 * * * *",
        ),
        notifyFailedKafkaEvents = NotifyFailedKafkaEvents.Config(
            disabled = true,
            maxRetries = 5,
            cronPattern = "0 */15 * ? * MON-FRI",
        ),
        updateApentForPamelding = UpdateApentForPamelding.Config(
            disabled = true,
            cronPattern = "0 55 23 * * *",
        ),
        generateUtbetaling = GenerateUtbetaling.Config(
            disabled = true,
            cronPattern = "0 0 5 7 * *",
        ),
    ),
    okonomi = OkonomiConfig(
        minimumTilsagnPeriodeStart = Tiltakskode.entries.associateWith { LocalDate.of(2025, 1, 1) },
    ),
    kontoregisterOrganisasjon = AuthenticatedHttpClientConfig(
        url = "http://localhost:8090",
        scope = "default",
    ),
    clamav = HttpClientConfig(
        url = "http://localhost:8090",
    ),
)
