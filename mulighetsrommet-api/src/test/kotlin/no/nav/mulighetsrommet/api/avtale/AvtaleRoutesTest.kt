package no.nav.mulighetsrommet.api.avtale

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import no.nav.mulighetsrommet.api.*
import no.nav.mulighetsrommet.api.clients.msgraph.AdGruppe
import no.nav.mulighetsrommet.api.clients.msgraph.mockMsGraphGetMemberGroups
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.ktor.createMockEngine
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

    fun appConfig(
        roller: Set<AdGruppeNavAnsattRolleMapping>,
    ) = createTestApplicationConfig().copy(
        auth = createAuthConfig(oauth, roles = setOf(generellRolle, avtaleSkrivRolle)),
        engine = createMockEngine {
            mockMsGraphGetMemberGroups(navAnsattOid) {
                roller.map { AdGruppe(id = it.adGruppeId, navn = it.rolle.name) }
            }
        },
    )

    val navAnsattClaims = mapOf(
        "NAVident" to "ABC123",
        "oid" to navAnsattOid.toString(),
    )

    context("hent avtaler") {
        test("401 Unauthorized for kall uten autentisering") {
            withTestApplication(appConfig(setOf())) {
                val response = client.get("/api/v1/intern/avtaler")
                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("200 OK når bruker har generell tilgang") {
            withTestApplication(appConfig(setOf(generellRolle))) {
                val response = client.get("/api/v1/intern/avtaler") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                }
                response.status shouldBe HttpStatusCode.OK
            }
        }
    }

    context("opprett avtale") {
        test("403 Forbidden når bruker mangler generell tilgang") {
            withTestApplication(appConfig(setOf(avtaleSkrivRolle))) {
                val response = client.put("/api/v1/intern/avtaler") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                }
                response.status shouldBe HttpStatusCode.Forbidden
                response.bodyAsText() shouldBe "Mangler følgende rolle: TILTAKADMINISTRASJON_GENERELL"
            }
        }

        test("403 Forbidden når bruker mangler skrivetilgang") {
            withTestApplication(appConfig(setOf(generellRolle))) {
                val response = client.put("/api/v1/intern/avtaler") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                }
                response.status shouldBe HttpStatusCode.Forbidden
            }
        }

        test("400 Bad Request når avtale mangler i request body") {
            withTestApplication(appConfig(setOf(generellRolle, avtaleSkrivRolle))) {
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
