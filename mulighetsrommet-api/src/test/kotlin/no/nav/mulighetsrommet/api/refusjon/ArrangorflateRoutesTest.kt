package no.nav.mulighetsrommet.api.refusjon

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.*
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
import no.nav.mulighetsrommet.api.createAuthConfig
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.createTestApplicationConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.refusjon.db.RefusjonskravDbo
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonKravAft
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonKravBeregningAft
import no.nav.mulighetsrommet.api.withTestApplication
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.dto.Kontonummer
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.time.LocalDateTime
import java.util.*

class ArrangorflateRoutesTest : FunSpec({
    val databaseConfig = createDatabaseTestConfig()
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
                periodeStart = LocalDateTime.of(2024, 8, 1, 0, 0),
                periodeSlutt = LocalDateTime.of(2024, 8, 31, 0, 0),
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
        domain.initialize(database.db)
    }

    afterSpec {
        oauth.shutdown()
    }

    fun appConfig(
        engine: HttpClientEngine = createMockEngine(
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
            },
        ),
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

    test("Utdatert refusjonskrav gir 400 i godkjenn-refusjon") {
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
                    GodkjennRefusjonskravAft(
                        belop = krav.beregning.output.belop + 200, // Feil belop
                        deltakelser = (krav.beregning as RefusjonKravBeregningAft).input.deltakelser,
                        betalingsinformasjon = GodkjennRefusjonskravAft.Betalingsinformasjon(
                            kontonummer = Kontonummer("12312312312"),
                            kid = null,
                        ),
                    ),
                )
            }
            response.status shouldBe HttpStatusCode.BadRequest
        }
    }
})
