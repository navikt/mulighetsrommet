package no.nav.mulighetsrommet.api.refusjon

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.altinn.AltinnClient
import no.nav.mulighetsrommet.altinn.AltinnClient.AuthorizedParty
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.clients.dokark.DokarkResponse
import no.nav.mulighetsrommet.api.clients.dokark.DokarkResponseDokument
import no.nav.mulighetsrommet.api.createAuthConfig
import no.nav.mulighetsrommet.api.createTestApplicationConfig
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.refusjon.db.RefusjonskravDbo
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonKravAft
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonKravBeregningAft
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonskravPeriode
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonskravStatus
import no.nav.mulighetsrommet.api.withTestApplication
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.domain.dto.Kontonummer
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class ArrangorflateRoutesTest : FunSpec({
    val databaseConfig = databaseConfig
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    val identMedTilgang = NorskIdent("01010199988")
    val hovedenhet = ArrangorDto(
        id = UUID.randomUUID(),
        organisasjonsnummer = Organisasjonsnummer("883674471"),
        navn = "Hovedenhet",
        postnummer = "0102",
        poststed = "Oslo",
    )
    val barnevernsNembda = ArrangorDto(
        id = UUID.randomUUID(),
        organisasjonsnummer = Organisasjonsnummer("973674471"),
        navn = "BARNEVERNS- OG HELSENEMNDA I BUSKERUD OG OMEGN",
        postnummer = "0102",
        poststed = "Oslo",
        overordnetEnhet = hovedenhet.organisasjonsnummer,
    )
    val krav = RefusjonskravDbo(
        id = UUID.randomUUID(),
        gjennomforingId = TiltaksgjennomforingFixtures.AFT1.id,
        fristForGodkjenning = LocalDateTime.now(),
        beregning = RefusjonKravBeregningAft(
            input = RefusjonKravBeregningAft.Input(
                periode = RefusjonskravPeriode.fromDayInMonth(LocalDate.of(2024, 8, 1)),
                sats = 20205,
                deltakelser = emptySet(),
            ),
            output = RefusjonKravBeregningAft.Output(
                belop = 0,
                deltakelser = emptySet(),
            ),
        ),
        kontonummer = Kontonummer("12312312312"),
        kid = null,
    )

    val domain = MulighetsrommetTestDomain(
        enheter = listOf(NavEnhetFixtures.IT, NavEnhetFixtures.Innlandet, NavEnhetFixtures.Gjovik),
        ansatte = listOf(NavAnsattFixture.ansatt1, NavAnsattFixture.ansatt2),
        tiltakstyper = listOf(TiltakstypeFixtures.AFT),
        avtaler = listOf(
            AvtaleFixtures.AFT.copy(
                arrangorId = hovedenhet.id,
                arrangorUnderenheter = listOf(barnevernsNembda.id),
            ),
        ),
        gjennomforinger = listOf(TiltaksgjennomforingFixtures.AFT1.copy(arrangorId = barnevernsNembda.id)),
        deltakere = emptyList(),
        arrangorer = listOf(hovedenhet, barnevernsNembda),
        refusjonskrav = listOf(krav),
    )
    val oauth = MockOAuth2Server()

    beforeSpec {
        oauth.start()
    }

    beforeEach {
        database.db.truncateAll()
        domain.initialize(database.db)
    }

    afterSpec {
        oauth.shutdown()
    }

    val altinnRequestHandler: Pair<String, suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData> =
        "/altinn/accessmanagement/api/v1/resourceowner/authorizedparties" to {
            val body = Json.decodeFromString<AltinnClient.AltinnRequest>(
                (it.body as TextContent).text,
            )
            if (body.value == identMedTilgang.value) {
                respondJson(
                    listOf(
                        AuthorizedParty(
                            organizationNumber = barnevernsNembda.organisasjonsnummer.value,
                            organizationName = barnevernsNembda.navn,
                            type = "type",
                            authorizedResources = listOf("tiltak-arrangor-refusjon"),
                            subunits = emptyList(),
                        ),
                    ),
                )
            } else {
                respondJson(emptyList<AuthorizedParty>())
            }
        }
    val dokarkRequestHandler: Pair<String, suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData> =
        "/dokark/rest/journalpostapi/v1/journalpost" to {
            respondJson(
                DokarkResponse(
                    journalpostId = "123",
                    journalstatus = "bra",
                    melding = null,
                    journalpostferdigstilt = true,
                    dokumenter = listOf(DokarkResponseDokument("123"))
                ),
            )
        }

    fun appConfig(
        requestHandlers: List<Pair<String, suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData>> =
            listOf(altinnRequestHandler, dokarkRequestHandler)
    ) = createTestApplicationConfig().copy(
        database = databaseConfig,
        auth = createAuthConfig(oauth, roles = emptyList()),
        engine = createMockEngine(*requestHandlers.toTypedArray()),
    )

    test("401 Unauthorized uten pid i claims") {
        withTestApplication(appConfig()) {
            val response = client.get("/api/v1/intern/arrangorflate/tilgang-arrangor") {
                bearerAuth(oauth.issueToken().serialize())
            }
            response.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    test("401 Unauthorized med pid uten tilgang") {
        withTestApplication(appConfig()) {
            val response = client.get("/api/v1/intern/arrangorflate/tilgang-arrangor") {
                bearerAuth(oauth.issueToken(claims = mapOf("pid" to "01010199922")).serialize())
            }
            response.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    test("200 og arrangor returneres på tilgang endepunkt") {
        withTestApplication(appConfig()) {
            val response = client.get("/api/v1/intern/arrangorflate/tilgang-arrangor") {
                bearerAuth(oauth.issueToken(claims = mapOf("pid" to identMedTilgang.value)).serialize())
                contentType(ContentType.Application.Json)
            }
            response.status shouldBe HttpStatusCode.OK
            val responseBody = response.bodyAsText()
            val tilganger: List<ArrangorDto> = Json.decodeFromString(responseBody)
            tilganger shouldHaveSize 1
            tilganger[0].organisasjonsnummer shouldBe barnevernsNembda.organisasjonsnummer
        }
    }

    test("401 hent krav uten tilgang til bedrift") {
        withTestApplication(appConfig()) {
            val response = client.get("/api/v1/intern/arrangorflate/refusjonskrav/${krav.id}") {
                bearerAuth(oauth.issueToken(claims = mapOf("pid" to "01010199922")).serialize())
                contentType(ContentType.Application.Json)
            }
            response.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    test("200 hent krav") {
        withTestApplication(appConfig()) {
            val response = client.get("/api/v1/intern/arrangorflate/refusjonskrav/${krav.id}") {
                bearerAuth(oauth.issueToken(claims = mapOf("pid" to identMedTilgang.value)).serialize())
                contentType(ContentType.Application.Json)
            }
            response.status shouldBe HttpStatusCode.OK
            val responseBody = response.bodyAsText()
            val kravResponse: RefusjonKravAft = Json.decodeFromString(responseBody)
            kravResponse.id shouldBe krav.id
        }
    }

    test("feil sjekksum ved godkjenning av refusjon gir 400") {
        withTestApplication(appConfig()) {
            val client = createClient {
                install(ContentNegotiation) {
                    json()
                }
            }
            val response = client.post("/api/v1/intern/arrangorflate/refusjonskrav/${krav.id}/godkjenn-refusjon") {
                bearerAuth(oauth.issueToken(claims = mapOf("pid" to identMedTilgang.value)).serialize())
                contentType(ContentType.Application.Json)
                setBody(
                    GodkjennRefusjonskrav(
                        digest = "d3b07384d113edec49eaa6238ad5ff00",
                        betalingsinformasjon = GodkjennRefusjonskrav.Betalingsinformasjon(
                            kontonummer = Kontonummer("12312312312"),
                            kid = null,
                        ),
                    ),
                )
            }
            response.status shouldBe HttpStatusCode.BadRequest
        }
    }

    test("riktig sjekksum ved godkjenning av refusjon gir 200") {
        withTestApplication(appConfig()) {
            val client = createClient {
                install(ContentNegotiation) {
                    json()
                }
            }
            val response = client.post("/api/v1/intern/arrangorflate/refusjonskrav/${krav.id}/godkjenn-refusjon") {
                bearerAuth(oauth.issueToken(claims = mapOf("pid" to identMedTilgang.value)).serialize())
                contentType(ContentType.Application.Json)
                setBody(
                    GodkjennRefusjonskrav(
                        digest = krav.beregning.getDigest(),
                        betalingsinformasjon = GodkjennRefusjonskrav.Betalingsinformasjon(
                            kontonummer = Kontonummer("12312312312"),
                            kid = null,
                        ),
                    ),
                )
            }
            response.status shouldBe HttpStatusCode.OK
        }
    }

    test("feil mot dokark ruller tilbake godkjenning") {
        withTestApplication(appConfig(
            listOf(
                altinnRequestHandler,
                "/dokark/rest/journalpostapi/v1/journalpost" to {
                    respondError(HttpStatusCode.InternalServerError)
                },
            )
        )) {
            val client = createClient {
                install(ContentNegotiation) {
                    json()
                }
            }
            var response = client.post("/api/v1/intern/arrangorflate/refusjonskrav/${krav.id}/godkjenn-refusjon") {
                bearerAuth(oauth.issueToken(claims = mapOf("pid" to identMedTilgang.value)).serialize())
                contentType(ContentType.Application.Json)
                setBody(
                    GodkjennRefusjonskrav(
                        digest = krav.beregning.getDigest(),
                        betalingsinformasjon = GodkjennRefusjonskrav.Betalingsinformasjon(
                            kontonummer = Kontonummer("12312312312"),
                            kid = null,
                        ),
                    ),
                )
            }
            response.status shouldBe HttpStatusCode.InternalServerError

            response = client.get("/api/v1/intern/arrangorflate/refusjonskrav/${krav.id}") {
                bearerAuth(oauth.issueToken(claims = mapOf("pid" to identMedTilgang.value)).serialize())
                contentType(ContentType.Application.Json)
            }
            response.status shouldBe HttpStatusCode.OK
            val responseBody = response.bodyAsText()
            val kravResponse: RefusjonKravAft = Json.decodeFromString(responseBody)
            kravResponse.status shouldBe RefusjonskravStatus.KLAR_FOR_GODKJENNING
        }
    }
})
