package no.nav.mulighetsrommet.api.avtale

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import no.nav.mulighetsrommet.api.*
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures
import no.nav.mulighetsrommet.api.navansatt.ktor.NavAnsattManglerTilgang
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.util.*

class AvtaleRoutesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val domain = MulighetsrommetTestDomain(
        navEnheter = listOf(NavEnhetFixtures.Innlandet, NavEnhetFixtures.Oslo),
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

    val generellRolle = AdGruppeNavAnsattRolleMapping(UUID.randomUUID(), Rolle.TILTAKADMINISTRASJON_GENERELL)
    val avtaleSkrivRolle = AdGruppeNavAnsattRolleMapping(UUID.randomUUID(), Rolle.AVTALER_SKRIV)

    val navAnsattOid = UUID.randomUUID()

    fun appConfig() = createTestApplicationConfig().copy(
        auth = createAuthConfig(oauth, roles = setOf(generellRolle, avtaleSkrivRolle)),
    )

    fun getClaims(roller: Set<AdGruppeNavAnsattRolleMapping>) = mapOf(
        "NAVident" to "B123456",
        "oid" to navAnsattOid.toString(),
        "sid" to UUID.randomUUID().toString(),
        "groups" to roller.map { it.adGruppeId.toString() },
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
                val navAnsattClaims = getClaims(setOf(generellRolle))

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

                val navAnsattClaims = getClaims(setOf(avtaleSkrivRolle))

                val response = client.put("/api/v1/intern/avtaler") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                }

                response.status shouldBe HttpStatusCode.Forbidden
                response.body<NavAnsattManglerTilgang>().missingRole shouldBe Rolle.TILTAKADMINISTRASJON_GENERELL
            }
        }

        test("403 Forbidden når bruker mangler skrivetilgang") {
            withTestApplication(appConfig()) {
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val navAnsattClaims = getClaims(setOf(generellRolle))

                val response = client.put("/api/v1/intern/avtaler") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                }

                response.status shouldBe HttpStatusCode.Forbidden
                response.body<NavAnsattManglerTilgang>().missingRole shouldBe Rolle.AVTALER_SKRIV
            }
        }

        test("400 Bad Request når avtale mangler i request body") {
            withTestApplication(appConfig()) {
                val navAnsattClaims = getClaims(setOf(generellRolle, avtaleSkrivRolle))

                val response = client.put("/api/v1/intern/avtaler") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                    contentType(ContentType.Application.Json)
                    setBody("{}")
                }
                response.status shouldBe HttpStatusCode.BadRequest
            }
        }
    }
})
