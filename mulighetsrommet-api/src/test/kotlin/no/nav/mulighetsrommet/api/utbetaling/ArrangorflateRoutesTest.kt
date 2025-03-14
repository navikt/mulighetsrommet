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
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotliquery.Query
import no.nav.amt.model.EndringAarsak
import no.nav.amt.model.Melding
import no.nav.mulighetsrommet.altinn.AltinnClient
import no.nav.mulighetsrommet.altinn.AltinnClient.AuthorizedParty
import no.nav.mulighetsrommet.altinn.AltinnClient.AuthorizedPartyType
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.arrangorflate.GodkjennUtbetaling
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrFlateUtbetaling
import no.nav.mulighetsrommet.api.clients.dokark.DokarkResponse
import no.nav.mulighetsrommet.api.clients.dokark.DokarkResponseDokument
import no.nav.mulighetsrommet.api.createAuthConfig
import no.nav.mulighetsrommet.api.createTestApplicationConfig
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.fixtures.TilsagnFixtures.setTilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnDbo
import no.nav.mulighetsrommet.api.tilsagn.model.ArrangorflateTilsagn
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFri
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.api.utbetaling.db.DeltakerDbo
import no.nav.mulighetsrommet.api.utbetaling.db.DeltakerForslag
import no.nav.mulighetsrommet.api.utbetaling.db.DeltakerForslag.Status
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelseManedsverk
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelsePeriode
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelsePerioder
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningForhandsgodkjent
import no.nav.mulighetsrommet.api.withTestApplication
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.ktor.MockEngineBuilder
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson
import no.nav.mulighetsrommet.model.DeltakerStatus
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Periode
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class ArrangorflateRoutesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val identMedTilgang = NorskIdent("01010199988")

    val hovedenhet = ArrangorFixtures.hovedenhet
    val underenhet = ArrangorFixtures.underenhet1

    val deltaker = DeltakerDbo(
        id = UUID.randomUUID(),
        gjennomforingId = GjennomforingFixtures.AFT1.id,
        startDato = GjennomforingFixtures.AFT1.startDato,
        sluttDato = GjennomforingFixtures.AFT1.sluttDato,
        registrertTidspunkt = GjennomforingFixtures.AFT1.startDato.atStartOfDay(),
        endretTidspunkt = LocalDateTime.now(),
        deltakelsesprosent = 100.0,
        deltakelsesmengder = listOf(),
        status = DeltakerStatus(
            type = DeltakerStatus.Type.DELTAR,
            aarsak = null,
            opprettetDato = LocalDateTime.now(),
        ),
    )

    val tilsagn = TilsagnDbo(
        id = UUID.randomUUID(),
        gjennomforingId = GjennomforingFixtures.AFT1.id,
        periode = Periode(LocalDate.of(2024, 6, 1), LocalDate.of(2025, 1, 1)),
        lopenummer = 1,
        bestillingsnummer = "A-2025/1-1",
        kostnadssted = NavEnhetFixtures.Innlandet.enhetsnummer,
        beregning = TilsagnBeregningFri(
            input = TilsagnBeregningFri.Input(1000),
            output = TilsagnBeregningFri.Output(1000),
        ),
        arrangorId = ArrangorFixtures.underenhet1.id,
        behandletAv = NavAnsattFixture.ansatt1.navIdent,
        behandletTidspunkt = LocalDateTime.now(),
        type = TilsagnType.TILSAGN,
    )

    val utbetaling = UtbetalingDbo(
        id = UUID.randomUUID(),
        gjennomforingId = GjennomforingFixtures.AFT1.id,
        fristForGodkjenning = LocalDateTime.now(),
        beregning = UtbetalingBeregningForhandsgodkjent(
            input = UtbetalingBeregningForhandsgodkjent.Input(
                periode = Periode.forMonthOf(LocalDate.of(2024, 8, 1)),
                sats = 20205,
                stengt = setOf(),
                deltakelser = setOf(
                    DeltakelsePerioder(
                        deltakelseId = deltaker.id,
                        perioder = listOf(
                            DeltakelsePeriode(
                                periode = Periode(LocalDate.of(2024, 8, 1), LocalDate.of(2024, 8, 31)),
                                deltakelsesprosent = 100.0,
                            ),
                        ),
                    ),
                ),
            ),
            output = UtbetalingBeregningForhandsgodkjent.Output(
                belop = 0,
                deltakelser = setOf(
                    DeltakelseManedsverk(
                        deltakelseId = deltaker.id,
                        manedsverk = 1.0,
                    ),
                ),
            ),
        ),
        kontonummer = Kontonummer("12312312312"),
        kid = null,
        periode = Periode.forMonthOf(LocalDate.of(2024, 8, 1)),
        innsender = null,
        beskrivelse = null,
    )

    val domain = MulighetsrommetTestDomain(
        navEnheter = listOf(NavEnhetFixtures.IT, NavEnhetFixtures.Innlandet, NavEnhetFixtures.Gjovik),
        ansatte = listOf(NavAnsattFixture.ansatt1, NavAnsattFixture.ansatt2),
        tiltakstyper = listOf(TiltakstypeFixtures.AFT),
        avtaler = listOf(
            AvtaleFixtures.AFT.copy(
                arrangor = AvtaleFixtures.AFT.arrangor?.copy(
                    hovedenhet = hovedenhet.id,
                    underenheter = listOf(underenhet.id),
                ),
            ),
        ),
        gjennomforinger = listOf(GjennomforingFixtures.AFT1.copy(arrangorId = underenhet.id)),
        deltakere = listOf(deltaker),
        arrangorer = listOf(hovedenhet, underenhet),
        tilsagn = listOf(tilsagn),
        utbetalinger = listOf(utbetaling),
    ) {
        setTilsagnStatus(tilsagn, TilsagnStatus.GODKJENT)
    }

    val oauth = MockOAuth2Server()

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

    fun MockEngineBuilder.mockAltinnAuthorizedParties() {
        post("/altinn/accessmanagement/api/v1/resourceowner/authorizedparties") {
            val body = Json.decodeFromString<AltinnClient.AltinnRequest>(
                (it.body as TextContent).text,
            )
            if (body.value == identMedTilgang.value) {
                respondJson(
                    listOf(
                        AuthorizedParty(
                            organizationNumber = underenhet.organisasjonsnummer.value,
                            name = underenhet.navn,
                            type = AuthorizedPartyType.Organization,
                            authorizedResources = listOf("nav_tiltaksarrangor_be-om-utbetaling"),
                            subunits = emptyList(),
                        ),
                    ),
                )
            } else {
                respondJson(emptyList<AuthorizedParty>())
            }
        }
    }

    fun MockEngineBuilder.mockJournalpost() {
        post("/dokark/rest/journalpostapi/v1/journalpost") {
            respondJson(
                DokarkResponse(
                    journalpostId = "123",
                    journalstatus = "bra",
                    melding = null,
                    journalpostferdigstilt = true,
                    dokumenter = listOf(DokarkResponseDokument("123")),
                ),
            )
        }
    }

    fun appConfig(
        engine: MockEngine = createMockEngine {
            mockAltinnAuthorizedParties()
            mockJournalpost()
        },
    ) = createTestApplicationConfig().copy(
        database = databaseConfig,
        auth = createAuthConfig(oauth, roles = emptyList()),
        engine = engine,
    )

    test("401 Unauthorized uten pid i claims") {
        withTestApplication(appConfig()) {
            val response = client.get("/api/v1/intern/arrangorflate/tilgang-arrangor") {
                bearerAuth(oauth.issueToken().serialize())
            }
            response.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    test("200 OK og tom liste med pid uten tilgang") {
        withTestApplication(appConfig()) {
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
        withTestApplication(appConfig()) {
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

    val orgnr = underenhet.organisasjonsnummer.value

    test("403 hent tilsagn uten tilgang til bedrift") {
        withTestApplication(appConfig()) {
            val response = client.get("/api/v1/intern/arrangorflate/arrangor/$orgnr/tilsagn") {
                bearerAuth(oauth.issueToken(claims = mapOf("pid" to "01010199922")).serialize())
                contentType(ContentType.Application.Json)
            }
            response.status shouldBe HttpStatusCode.Forbidden
        }
    }

    test("200 hent tilsagn med tilgang til bedrift") {
        withTestApplication(appConfig()) {
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

            response.body<List<ArrangorflateTilsagn>>().shouldHaveSize(1).first().should {
                it.status shouldBe ArrangorflateTilsagn.StatusOgAarsaker(
                    status = TilsagnStatus.GODKJENT,
                    aarsaker = listOf(),
                )
            }
        }
    }

    test("403 hent utbetaling uten tilgang til bedrift") {
        withTestApplication(appConfig()) {
            val response = client.get("/api/v1/intern/arrangorflate/utbetaling/${utbetaling.id}") {
                bearerAuth(oauth.issueToken(claims = mapOf("pid" to "01010199922")).serialize())
                contentType(ContentType.Application.Json)
            }
            response.status shouldBe HttpStatusCode.Forbidden
        }
    }

    test("200 hent utbetaling") {
        withTestApplication(appConfig()) {
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
        withTestApplication(appConfig()) {
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
        withTestApplication(appConfig()) {
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
        withTestApplication(appConfig()) {
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
        val clientEngine = createMockEngine {
            mockAltinnAuthorizedParties()

            post("/dokark/rest/journalpostapi/v1/journalpost") {
                respondError(HttpStatusCode.InternalServerError)
            }
        }

        withTestApplication(appConfig(clientEngine)) {
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

        withTestApplication(appConfig()) {
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
