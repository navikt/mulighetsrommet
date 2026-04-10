package no.nav.mulighetsrommet.api.arrangor.api

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import no.nav.mulighetsrommet.api.createAuthConfig
import no.nav.mulighetsrommet.api.createTestApplicationConfig
import no.nav.mulighetsrommet.api.plugins.AppRoles
import no.nav.mulighetsrommet.api.withTestApplication
import no.nav.mulighetsrommet.brreg.BrregUnderenhetDto
import no.nav.mulighetsrommet.brreg.testFixture.BrregFixtures
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson
import no.nav.security.mock.oauth2.MockOAuth2Server

class ArrangorPublicRoutesTest : FunSpec({
    val oauth = MockOAuth2Server()

    beforeSpec {
        oauth.start()
    }

    afterSpec {
        oauth.shutdown()
    }

    fun appConfig() = createTestApplicationConfig().copy(
        auth = createAuthConfig(oauth, roles = setOf()),
    )

    context("/v1/arrangor") {
        context("sok underenhet") {
            val sokUrl = { term: String -> "/api/v1/arrangor/underenhet?sok=$term" }

            test("401 når påkrevde claims mangler fra token") {
                withTestApplication(appConfig()) {
                    val term = "Bedrift"
                    val response = client.get(sokUrl(term)) {
                        bearerAuth(
                            oauth.issueToken(claims = withApplicationRoles()).serialize(),
                        )
                    }

                    response.status shouldBe HttpStatusCode.Unauthorized
                }
            }

            test("200 når søk returneres") {
                val localAppConfig = appConfig().copy(
                    engine = createMockEngine {
                        get("https://data.brreg.no/enhetsregisteret/api/underenheter") {
                            respondJson(BrregFixtures.SOK_UNDERENHET)
                        }
                    },
                )
                withTestApplication(localAppConfig) {
                    val term = "Bedrift"
                    val response = client.get(sokUrl(term)) {
                        bearerAuth(
                            oauth.issueToken(claims = withApplicationRoles(AppRoles.READ_GJENNOMFORING)).serialize(),
                        )
                    }

                    response.status shouldBe HttpStatusCode.OK

                    val responseBody = response.body<List<BrregUnderenhetDto>>()
                    responseBody.size shouldBe 1
                }
            }

            test("200 tom liste ved tomt resultat") {
                val localAppConfig = appConfig().copy(
                    engine = createMockEngine {
                        get("https://data.brreg.no/enhetsregisteret/api/underenheter") {
                            respondJson(BrregFixtures.SOK_UNDERENHET_INGEN_TREFF)
                        }
                    },
                )
                withTestApplication(localAppConfig) {
                    val term = "Bedrift"
                    val response = client.get(sokUrl(term)) {
                        bearerAuth(
                            oauth.issueToken(claims = withApplicationRoles(AppRoles.READ_GJENNOMFORING)).serialize(),
                        )
                    }

                    response.status shouldBe HttpStatusCode.OK

                    val responseBody = response.body<List<BrregUnderenhetDto>>()
                    responseBody.size shouldBe 0
                }
            }
        }
    }
})

private fun withApplicationRoles(roles: String? = null): Map<String, List<String>> = mapOf(
    "roles" to listOfNotNull(AppRoles.ACCESS_AS_APPLICATION, roles),
)
