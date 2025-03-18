package no.nav.mulighetsrommet.api.avtale

import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures
import no.nav.mulighetsrommet.api.test.AuthTestUtils.addAuth
import no.nav.mulighetsrommet.api.test.TestBase
import no.nav.mulighetsrommet.api.withTestApplication

/**
 * Refactored version of AvtaleRoutesTest using the new TestBase
 */
class RefactoredAvtaleRoutesTest : TestBase() {
    // Override domain to provide the specific setup needed for this test
    override val domain = MulighetsrommetTestDomain(
        navEnheter = listOf(NavEnhetFixtures.Innlandet, NavEnhetFixtures.Oslo),
        avtaler = emptyList(),
    )

    init {
        test("401 Unauthorized for uautentisert kall for PUT av avtaledata") {
            withTestApplication(appConfig()) {
                val response = client.put("/api/v1/intern/avtaler")
                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("401 Unauthorized for uautentisert kall for PUT av avtaledata når bruker ikke har tilgang til å skrive for avtaler") {
            withTestApplication(appConfig()) {
                val response = client.put("/api/v1/intern/avtaler") {
                    addAuth(oauth) // No roles specified
                }
                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("401 Unauthorized for uautentisert kall for PUT av avtaledata når bruker har tilgang til å skrive for avtaler, men mangler generell tilgang") {
            withTestApplication(appConfig()) {
                val response = client.put("/api/v1/intern/avtaler") {
                    addAuth(oauth, groups = listOf(generellRolle.adGruppeId))
                }
                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("200 OK for autentisert kall for GET av avtaledata") {
            withTestApplication(appConfig()) {
                val response = client.get("/api/v1/intern/avtaler") {
                    addAuth(oauth, groups = listOf(generellRolle.adGruppeId))
                }
                response.status shouldBe HttpStatusCode.OK
            }
        }
    }
}
