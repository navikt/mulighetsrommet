package no.nav.mulighetsrommet.api.avtale

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import no.nav.mulighetsrommet.api.*
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSats
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSatsDto
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures
import no.nav.mulighetsrommet.api.navansatt.ktor.NavAnsattManglerTilgang
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.time.LocalDate
import java.util.*

class AvtaleRoutesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val ansatt = NavAnsattFixture.DonaldDuck
    val domain = MulighetsrommetTestDomain(
        navEnheter = listOf(NavEnhetFixtures.Innlandet, NavEnhetFixtures.Oslo),
        ansatte = listOf(ansatt),
        avtaler = emptyList(),
    )

    val oauth = MockOAuth2Server()

    beforeSpec {
        oauth.start()
        domain.initialize(database.db)
    }

    afterSpec {
        oauth.shutdown()
    }

    val generellRolle = EntraGroupNavAnsattRolleMapping(UUID.randomUUID(), Rolle.TILTAKADMINISTRASJON_GENERELL)
    val avtaleSkrivRolle = EntraGroupNavAnsattRolleMapping(UUID.randomUUID(), Rolle.AVTALER_SKRIV)

    fun appConfig() = createTestApplicationConfig().copy(
        auth = createAuthConfig(oauth, roles = setOf(generellRolle, avtaleSkrivRolle)),
    )

    context("hent avtaler") {
        test("401 Unauthorized for kall uten autentisering") {
            withTestApplication(appConfig()) {
                val response = client.get("/api/v1/intern/avtaler")
                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("200 OK når bruker har generell tilgang") {
            withTestApplication(appConfig()) {
                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle))

                val response = client.get("/api/v1/intern/avtaler") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                }
                response.status shouldBe HttpStatusCode.OK
            }
        }
    }

    context("opprett avtale") {
        test("403 Forbidden når bruker mangler generell tilgang") {
            withTestApplication(appConfig()) {
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val navAnsattClaims = getAnsattClaims(ansatt, setOf(avtaleSkrivRolle))

                val response = client.put("/api/v1/intern/avtaler") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                }

                response.status shouldBe HttpStatusCode.Forbidden
                response.body<NavAnsattManglerTilgang>().missingRoles shouldBe setOf(Rolle.TILTAKADMINISTRASJON_GENERELL)
            }
        }

        test("403 Forbidden når bruker mangler skrivetilgang") {
            withTestApplication(appConfig()) {
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle))

                val response = client.put("/api/v1/intern/avtaler") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                }

                response.status shouldBe HttpStatusCode.Forbidden
                response.body<NavAnsattManglerTilgang>().missingRoles shouldBe setOf(Rolle.AVTALER_SKRIV)
            }
        }

        test("400 Bad Request når avtale mangler i request body") {
            withTestApplication(appConfig()) {
                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle, avtaleSkrivRolle))

                val response = client.put("/api/v1/intern/avtaler") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                    contentType(ContentType.Application.Json)
                    setBody("{}")
                }
                response.status shouldBe HttpStatusCode.BadRequest
            }
        }
    }

    context("hent avtalte satser") {
        beforeEach {
            MulighetsrommetTestDomain(
                avtaler = listOf(
                    AvtaleFixtures.AFT,
                    AvtaleFixtures.oppfolging.copy(
                        prismodell = PrismodellType.AVTALT_PRIS_PER_MANEDSVERK,
                        satser = listOf(AvtaltSats(LocalDate.of(2025, 1, 1), 1000)),
                    ),
                ),
            ).initialize(database.db)
        }

        test("henter avtalte satser fra avtalens prismodell") {
            withTestApplication(appConfig()) {
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle))

                val response1 = client.get("/api/v1/intern/avtaler/${AvtaleFixtures.AFT.id}/satser") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                }
                response1.status shouldBe HttpStatusCode.OK
                response1.body<List<AvtaltSatsDto>>() shouldBe listOf(
                    AvtaltSatsDto(
                        gjelderFra = LocalDate.of(2025, 1, 1),
                        pris = 20_975,
                        valuta = "NOK",
                    ),
                )

                val response2 = client.get("/api/v1/intern/avtaler/${AvtaleFixtures.oppfolging.id}/satser") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                }
                response2.status shouldBe HttpStatusCode.OK
                response2.body<List<AvtaltSatsDto>>() shouldBe listOf(
                    AvtaltSatsDto(
                        gjelderFra = LocalDate.of(2025, 1, 1),
                        pris = 1000,
                        valuta = "NOK",
                    ),
                )
            }
        }
    }
})
