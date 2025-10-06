package no.nav.mulighetsrommet.api

import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.utils.io.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.util.KafkaPropertiesBuilder
import no.nav.common.kafka.util.KafkaPropertiesBuilder.consumerBuilder
import no.nav.mulighetsrommet.api.avtale.task.NotifySluttdatoForAvtalerNarmerSeg
import no.nav.mulighetsrommet.api.clients.pdl.GraphqlRequest
import no.nav.mulighetsrommet.api.clients.pdl.GraphqlRequest.Identer
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.gjennomforing.task.NotifySluttdatoForGjennomforingerNarmerSeg
import no.nav.mulighetsrommet.api.gjennomforing.task.UpdateApentForPamelding
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.navansatt.service.NavAnsattSyncService
import no.nav.mulighetsrommet.api.navansatt.task.SynchronizeNavAnsatte
import no.nav.mulighetsrommet.api.navenhet.task.SynchronizeNorgEnheter
import no.nav.mulighetsrommet.api.utbetaling.task.GenerateUtbetaling
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import no.nav.mulighetsrommet.featuretoggle.service.UnleashFeatureToggleService
import no.nav.mulighetsrommet.metrics.Metrics
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import no.nav.mulighetsrommet.tokenprovider.TexasClient
import no.nav.mulighetsrommet.tokenprovider.TokenReponse
import no.nav.mulighetsrommet.utdanning.task.SynchronizeUtdanninger
import no.nav.mulighetsrommet.utils.toUUID
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.ByteArraySerializer
import java.time.LocalDate

private val adGruppeForLokalUtvikling = "52bb9196-b071-4cc7-9472-be4942d33c4b".toUUID()

val ApplicationConfigLocal = AppConfig(
    database = DatabaseConfig(
        jdbcUrl = "jdbc:postgresql://localhost:5442/mr-api?user=valp&password=valp",
        maximumPoolSize = 10,
        micrometerRegistry = Metrics.micrometerRegistry,
    ),
    flyway = FlywayMigrationManager.MigrationConfig(
        strategy = FlywayMigrationManager.InitializationStrategy.Migrate,
    ),
    kafka = KafkaConfig(
        producerProperties = KafkaPropertiesBuilder.producerBuilder()
            .withBaseProperties()
            .withProducerId("mulighetsrommet-api-kafka-producer.v1")
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
    ),
    auth = AuthConfig(
        azure = AuthProvider(
            issuer = "http://localhost:8081/azure",
            jwksUri = "http://localhost:8081/azure/jwks",
            audience = "mulighetsrommet-api",
            tokenEndpointUrl = "http://localhost:8081/azure/token",
            privateJwk = "azure",
        ),
        tokenx = AuthProvider(
            issuer = "http://localhost:8081/tokenx",
            jwksUri = "http://localhost:8081/tokenx/jwks",
            audience = "mulighetsrommet-api",
            tokenEndpointUrl = "http://localhost:8081/tokenx/token",
            privateJwk = "tokenx",
        ),
        maskinporten = AuthProvider(
            issuer = "http://localhost:8081/maskinporten",
            jwksUri = "http://localhost:8081/maskinporten/jwks",
            audience = "mulighetsrommet-api",
            tokenEndpointUrl = "http://localhost:8081/maskinporten/token",
            privateJwk = "maskinporten",
        ),
        roles = setOf(
            EntraGroupNavAnsattRolleMapping(adGruppeForLokalUtvikling, Rolle.TEAM_MULIGHETSROMMET),
            EntraGroupNavAnsattRolleMapping(adGruppeForLokalUtvikling, Rolle.AVTALER_SKRIV),
            EntraGroupNavAnsattRolleMapping(adGruppeForLokalUtvikling, Rolle.TILTAKADMINISTRASJON_GENERELL),
            EntraGroupNavAnsattRolleMapping(adGruppeForLokalUtvikling, Rolle.OPPFOLGER_GJENNOMFORING),
            EntraGroupNavAnsattRolleMapping(adGruppeForLokalUtvikling, Rolle.TILTAKSGJENNOMFORINGER_SKRIV),
            EntraGroupNavAnsattRolleMapping(adGruppeForLokalUtvikling, Rolle.SAKSBEHANDLER_OKONOMI),
            EntraGroupNavAnsattRolleMapping(adGruppeForLokalUtvikling, Rolle.BESLUTTER_TILSAGN),
            EntraGroupNavAnsattRolleMapping(adGruppeForLokalUtvikling, Rolle.ATTESTANT_UTBETALING),
            EntraGroupNavAnsattRolleMapping(adGruppeForLokalUtvikling, Rolle.KONTAKTPERSON),
        ),
        texas = TexasClient.Config(
            tokenEndpoint = "http://localhost:8090/api/v1/token",
            tokenExchangeEndpoint = "http://localhost:8090/api/v1/token/exchange",
            tokenIntrospectionEndpoint = "http://localhost:8090/api/v1/introspect",
            engine = MockEngine { _ ->
                respond(
                    content = Json.encodeToString(
                        TokenReponse(
                            access_token = "dummy",
                            token_type = TokenReponse.TokenType.Bearer,
                            expires_in = 1_000_1000,
                        ),
                    ),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
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
    unleash = UnleashFeatureToggleService.Config(
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
        updateApentForPamelding = UpdateApentForPamelding.Config(
            disabled = true,
            cronPattern = "0 55 23 * * *",
        ),
        generateUtbetaling = GenerateUtbetaling.Config(
            disabled = false,
            cronPattern = "0 0 5 7 * *",
        ),
    ),
    okonomi = OkonomiConfig(
        gyldigTilsagnPeriode = Tiltakskode.entries.associateWith {
            Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2030, 1, 1))
        },
    ),
    kontoregisterOrganisasjon = AuthenticatedHttpClientConfig(
        url = "http://localhost:8090",
        scope = "default",
    ),
    clamav = HttpClientConfig(
        url = "http://localhost:8090",
    ),
)
