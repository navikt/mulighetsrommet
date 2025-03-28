package no.nav.mulighetsrommet.api.avtale

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.mulighetsrommet.api.*
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsattRolle
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

    val generellRolle = AdGruppeNavAnsattRolleMapping(UUID.randomUUID(), NavAnsattRolle.TILTAKADMINISTRASJON_GENERELL)
    val avtaleSkrivRolle = AdGruppeNavAnsattRolleMapping(UUID.randomUUID(), NavAnsattRolle.AVTALER_SKRIV)

    fun appConfig() = createTestApplicationConfig().copy(
        auth = createAuthConfig(oauth, roles = listOf(generellRolle, avtaleSkrivRolle)),
    )

    test("401 Unauthorized for uautentisert kall for PUT av avtaledata") {
        withTestApplication(appConfig()) {
            val response = client.put("/api/v1/intern/avtaler")
            response.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    test("401 Unauthorized for uautentisert kall for PUT av avtaledata når bruker ikke har tilgang til å skrive for avtaler") {
        withTestApplication(appConfig()) {
            val response = client.put("/api/v1/intern/avtaler") {
                val claims = mapOf(
                    "NAVident" to "ABC123",
                    "groups" to emptyList<String>(),
                )
                bearerAuth(
                    oauth.issueToken(claims = claims).serialize(),
                )
            }
            response.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    test("401 Unauthorized for uautentisert kall for PUT av avtaledata når bruker har tilgang til å skrive for avtaler, men mangler generell tilgang") {
        withTestApplication(appConfig()) {
            val response = client.put("/api/v1/intern/avtaler") {
                val claims = mapOf(
                    "NAVident" to "ABC123",
                    "groups" to listOf(generellRolle.adGruppeId),
                )
                bearerAuth(
                    oauth.issueToken(claims = claims).serialize(),
                )
            }
            response.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    test("200 OK for autentisert kall for GET av avtaledata") {
        withTestApplication(appConfig()) {
            val response = client.get("/api/v1/intern/avtaler") {
                val claims = mapOf(
                    "NAVident" to "ABC123",
                    "groups" to listOf(generellRolle.adGruppeId),
                )
                bearerAuth(
                    oauth.issueToken(claims = claims).serialize(),
                )
            }
            response.status shouldBe HttpStatusCode.OK
        }
    }
})
