package no.nav.mulighetsrommet.api.utbetaling

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotliquery.Query
import no.nav.amt.model.EndringAarsak
import no.nav.amt.model.Melding
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.arrangorflate.api.ArrFlateUtbetaling
import no.nav.mulighetsrommet.api.arrangorflate.api.ArrangorflateTilsagnDto
import no.nav.mulighetsrommet.api.arrangorflate.api.GodkjennUtbetaling
import no.nav.mulighetsrommet.api.clients.dokark.DokarkResponse
import no.nav.mulighetsrommet.api.clients.dokark.DokarkResponseDokument
import no.nav.mulighetsrommet.api.createAuthConfig
import no.nav.mulighetsrommet.api.createTestApplicationConfig
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnDbo
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFri
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.utbetaling.db.DeltakerForslag
import no.nav.mulighetsrommet.api.utbetaling.db.DeltakerForslag.Status
import no.nav.mulighetsrommet.api.withTestApplication
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.util.*

class ArrangorflateRoutesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))
    val oauth = MockOAuth2Server()

    // Use test utils to create test data
    val deltaker = ArrangorflateTestUtils.createTestDeltaker()
    val tilsagn = ArrangorflateTestUtils.createTestTilsagn()
    val utbetaling = ArrangorflateTestUtils.createTestUtbetalingForhandsgodkjent(deltaker.id)

    // Set up domain with test data
    val domain = ArrangorflateTestUtils.createTestDomain(
        deltaker = deltaker,
        tilsagn = tilsagn,
        utbetalinger = listOf(utbetaling),
    )

    val identMedTilgang = ArrangorflateTestUtils.identMedTilgang
    val underenhet = ArrangorflateTestUtils.underenhet
    val orgnr = underenhet.organisasjonsnummer.value

    beforeSpec {
        oauth.start()
    }

    beforeEach {
        domain.initialize(database.db)
    }

    afterEach {
        database.truncateAll()
    }

    afterSpec {
        oauth.shutdown()
    }

    test("401 Unauthorized uten pid i claims") {
        withTestApplication(ArrangorflateTestUtils.appConfig(oauth)) {
            val response = client.get("/api/v1/intern/arrangorflate/tilgang-arrangor") {
                bearerAuth(oauth.issueToken().serialize())
            }
            response.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    test("200 OK og tom liste med pid uten tilgang") {
        withTestApplication(ArrangorflateTestUtils.appConfig(oauth)) {
            val client = createClient {
                install(ContentNegotiation) {
                    json()
                }
            }

            val response = client.get("/api/v1/intern/arrangorflate/tilgang-arrangor") {
                bearerAuth(oauth.issueToken(claims = mapOf("pid" to "01010199922")).serialize())
            }

            response.status shouldBe HttpStatusCode.OK
            response.body<List<ArrangorDto>>().shouldHaveSize(0)
        }
    }

    test("200 og arrangor returneres på tilgang endepunkt") {
        withTestApplication(ArrangorflateTestUtils.appConfig(oauth)) {
            val client = createClient {
                install(ContentNegotiation) {
                    json()
                }
            }

            val response = client.get("/api/v1/intern/arrangorflate/tilgang-arrangor") {
                bearerAuth(oauth.issueToken(claims = mapOf("pid" to identMedTilgang.value)).serialize())
                contentType(ContentType.Application.Json)
            }

            response.status shouldBe HttpStatusCode.OK

            val tilganger: List<ArrangorDto> = response.body()
            tilganger.shouldHaveSize(1).first().should {
                it.organisasjonsnummer shouldBe underenhet.organisasjonsnummer
            }
        }
    }

    test("403 hent tilsagn uten tilgang til bedrift") {
        withTestApplication(ArrangorflateTestUtils.appConfig(oauth)) {
            val response = client.get("/api/v1/intern/arrangorflate/arrangor/$orgnr/tilsagn") {
                bearerAuth(oauth.issueToken(claims = mapOf("pid" to "01010199922")).serialize())
                contentType(ContentType.Application.Json)
            }
            response.status shouldBe HttpStatusCode.Forbidden
        }
    }

    test("200 hent tilsagn med tilgang til bedrift") {
        withTestApplication(ArrangorflateTestUtils.appConfig(oauth)) {
            val client = createClient {
                install(ContentNegotiation) {
                    json()
                }
            }

            val response = client.get("/api/v1/intern/arrangorflate/arrangor/$orgnr/tilsagn") {
                bearerAuth(oauth.issueToken(claims = mapOf("pid" to identMedTilgang.value)).serialize())
                contentType(ContentType.Application.Json)
            }

            response.status shouldBe HttpStatusCode.OK

            response.body<List<ArrangorflateTilsagnDto>>().shouldHaveSize(1).first().should {
                it.status shouldBe ArrangorflateTilsagnDto.StatusOgAarsaker(
                    status = TilsagnStatus.GODKJENT,
                    aarsaker = listOf(),
                )
            }
        }
    }

    test("403 hent utbetaling uten tilgang til bedrift") {
        withTestApplication(ArrangorflateTestUtils.appConfig(oauth)) {
            val response = client.get("/api/v1/intern/arrangorflate/utbetaling/${utbetaling.id}") {
                bearerAuth(oauth.issueToken(claims = mapOf("pid" to "01010199922")).serialize())
                contentType(ContentType.Application.Json)
            }
            response.status shouldBe HttpStatusCode.Forbidden
        }
    }

    test("200 hent utbetaling") {
        withTestApplication(ArrangorflateTestUtils.appConfig(oauth)) {
            val client = createClient {
                install(ContentNegotiation) {
                    json()
                }
            }

            val response = client.get("/api/v1/intern/arrangorflate/utbetaling/${utbetaling.id}") {
                bearerAuth(oauth.issueToken(claims = mapOf("pid" to identMedTilgang.value)).serialize())
                contentType(ContentType.Application.Json)
            }

            response.status shouldBe HttpStatusCode.OK

            response.body<ArrFlateUtbetaling>().should {
                it.id shouldBe utbetaling.id
            }
        }
    }

    test("400 ved feil sjekksum ved godkjenning av utbetaling") {
        withTestApplication(ArrangorflateTestUtils.appConfig(oauth)) {
            val client = createClient {
                install(ContentNegotiation) {
                    json()
                }
            }

            val response = client.post("/api/v1/intern/arrangorflate/utbetaling/${utbetaling.id}/godkjenn") {
                bearerAuth(oauth.issueToken(claims = mapOf("pid" to identMedTilgang.value)).serialize())
                contentType(ContentType.Application.Json)
                setBody(
                    GodkjennUtbetaling(
                        digest = "d3b07384d113edec49eaa6238ad5ff00",
                        betalingsinformasjon = GodkjennUtbetaling.Betalingsinformasjon(
                            kontonummer = Kontonummer("12312312312"),
                            kid = null,
                        ),
                    ),
                )
            }

            response.status shouldBe HttpStatusCode.BadRequest

            response.body<JsonElement>() shouldBe Json.parseToJsonElement(
                """
                {
                  "type": "validation-error",
                  "status": 400,
                  "title": "Validation error",
                  "detail": "Unknown Validation Error",
                  "errors": [
                    {
                      "pointer": "/info",
                      "detail": "Informasjonen i kravet har endret seg. Vennligst se over på nytt."
                    }
                  ]
                }
                """,
            )
        }
    }

    // TODO: flytt resten av godkjenning-testene til egen testklasse for ArrangorflateService
    test("riktig sjekksum ved godkjenning av utbetaling gir 200, og spawner journalforing task") {
        withTestApplication(ArrangorflateTestUtils.appConfig(oauth)) {
            val client = createClient {
                install(ContentNegotiation) {
                    json()
                }
            }
            val response = client.post("/api/v1/intern/arrangorflate/utbetaling/${utbetaling.id}/godkjenn") {
                bearerAuth(oauth.issueToken(claims = mapOf("pid" to identMedTilgang.value)).serialize())
                contentType(ContentType.Application.Json)
                setBody(
                    GodkjennUtbetaling(
                        digest = utbetaling.beregning.getDigest(),
                        betalingsinformasjon = GodkjennUtbetaling.Betalingsinformasjon(
                            kontonummer = Kontonummer("12312312312"),
                            kid = null,
                        ),
                    ),
                )
            }
            response.status shouldBe HttpStatusCode.OK

            val count = database.run {
                session.single(Query("select count(*) from scheduled_tasks where task_name = 'JournalforUtbetaling'")) {
                    it.int("count")
                }
            }
            count shouldBe 1
        }
    }

    test("kan ikke godkjenne allerede godkjent") {
        withTestApplication(ArrangorflateTestUtils.appConfig(oauth)) {
            val client = createClient {
                install(ContentNegotiation) {
                    json()
                }
            }
            var response = client.post("/api/v1/intern/arrangorflate/utbetaling/${utbetaling.id}/godkjenn") {
                bearerAuth(oauth.issueToken(claims = mapOf("pid" to identMedTilgang.value)).serialize())
                contentType(ContentType.Application.Json)
                setBody(
                    GodkjennUtbetaling(
                        digest = utbetaling.beregning.getDigest(),
                        betalingsinformasjon = GodkjennUtbetaling.Betalingsinformasjon(
                            kontonummer = Kontonummer("12312312312"),
                            kid = null,
                        ),
                    ),
                )
            }
            response.status shouldBe HttpStatusCode.OK

            response = client.post("/api/v1/intern/arrangorflate/utbetaling/${utbetaling.id}/godkjenn") {
                bearerAuth(oauth.issueToken(claims = mapOf("pid" to identMedTilgang.value)).serialize())
                contentType(ContentType.Application.Json)
                setBody(
                    GodkjennUtbetaling(
                        digest = utbetaling.beregning.getDigest(),
                        betalingsinformasjon = GodkjennUtbetaling.Betalingsinformasjon(
                            kontonummer = Kontonummer("12312312312"),
                            kid = null,
                        ),
                    ),
                )
            }
            response.status shouldBe HttpStatusCode.BadRequest
        }
    }

    test("feil mot dokark gir fortsatt 200 på godkjenn siden det skjer i en task") {
        // Create custom engine to test error scenario
        val clientEngine = createMockEngine {
            ArrangorflateTestUtils.mockAltinnAuthorizedParties(this)

            post("/dokark/rest/journalpostapi/v1/journalpost") {
                respondError(HttpStatusCode.InternalServerError)
            }
        }

        // Use custom app config with error-producing engine
        val errorConfig = ArrangorflateTestUtils.appConfig(oauth, engine = clientEngine)

        withTestApplication(errorConfig) {
            val client = createClient {
                install(ContentNegotiation) {
                    json()
                }
            }
            val response = client.post("/api/v1/intern/arrangorflate/utbetaling/${utbetaling.id}/godkjenn") {
                bearerAuth(oauth.issueToken(claims = mapOf("pid" to identMedTilgang.value)).serialize())
                contentType(ContentType.Application.Json)
                setBody(
                    GodkjennUtbetaling(
                        digest = utbetaling.beregning.getDigest(),
                        betalingsinformasjon = GodkjennUtbetaling.Betalingsinformasjon(
                            kontonummer = Kontonummer("12312312312"),
                            kid = null,
                        ),
                    ),
                )
            }
            response.status shouldBe HttpStatusCode.OK
        }
    }

    test("ikke lov å godkjenne når det finnes relevante forslag") {
        database.run {
            queries.deltakerForslag.upsert(
                DeltakerForslag(
                    id = UUID.randomUUID(),
                    deltakerId = deltaker.id,
                    endring = Melding.Forslag.Endring.AvsluttDeltakelse(
                        aarsak = EndringAarsak.Syk,
                        harDeltatt = false,
                    ),
                    status = Status.VENTER_PA_SVAR,
                ),
            )
        }

        withTestApplication(ArrangorflateTestUtils.appConfig(oauth)) {
            val client = createClient {
                install(ContentNegotiation) {
                    json()
                }
            }

            val response = client.post("/api/v1/intern/arrangorflate/utbetaling/${utbetaling.id}/godkjenn") {
                bearerAuth(oauth.issueToken(claims = mapOf("pid" to identMedTilgang.value)).serialize())
                contentType(ContentType.Application.Json)
                setBody(
                    GodkjennUtbetaling(
                        digest = utbetaling.beregning.getDigest(),
                        betalingsinformasjon = GodkjennUtbetaling.Betalingsinformasjon(
                            kontonummer = Kontonummer("12312312312"),
                            kid = null,
                        ),
                    ),
                )
            }

            response.status shouldBe HttpStatusCode.BadRequest
            response.body<JsonElement>() shouldBe Json.parseToJsonElement(
                """
                {
                  "type": "validation-error",
                  "status": 400,
                  "title": "Validation error",
                  "detail": "Unknown Validation Error",
                  "errors": [
                    {
                      "pointer": "/info",
                      "detail": "Det finnes forslag på deltakere som påvirker utbetalingen. Disse må behandles av Nav før utbetalingen kan sendes inn."
                    }
                  ]
                }
                """,
            )
        }
    }
})
