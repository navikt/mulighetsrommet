package no.nav.mulighetsrommet.api.arrangor.api

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import no.nav.mulighetsrommet.api.createAuthConfig
import no.nav.mulighetsrommet.api.createTestApplicationConfig
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures
import no.nav.mulighetsrommet.api.plugins.AppRoles
import no.nav.mulighetsrommet.api.withTestApplication
import no.nav.mulighetsrommet.brreg.BrregHovedenhetDto
import no.nav.mulighetsrommet.brreg.BrregUnderenhetDto
import no.nav.mulighetsrommet.brreg.testFixture.BrregFixtures
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.security.mock.oauth2.MockOAuth2Server

class ArrangorPublicRoutesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val oauth = MockOAuth2Server()

    val domain = MulighetsrommetTestDomain(
        navEnheter = listOf(NavEnhetFixtures.Innlandet),
        ansatte = listOf(NavAnsattFixture.DonaldDuck),
        arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
        avtaler = emptyList(),
        gjennomforinger = emptyList(),
    )

    beforeSpec {
        oauth.start()

        domain.initialize(database.db)
    }

    afterSpec {
        oauth.shutdown()
    }

    fun appConfig() = createTestApplicationConfig().copy(
        auth = createAuthConfig(oauth, roles = setOf()),
    )

    context("/v1/arrangor") {
        context("sok hovedenhet") {
            val sokUrl = { term: String -> "/api/v1/arrangor/hovedenhet/sok/$term" }

            test("401 når påkrevde claims mangler fra token") {
                withTestApplication(appConfig()) {
                    val term = "Tiger"
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
                        get("https://data.brreg.no/enhetsregisteret/api/enheter") {
                            respondJson(BrregFixtures.SOK_ENHET)
                        }
                    },
                )
                withTestApplication(localAppConfig) {
                    val term = "Tiger"
                    val response = client.get(sokUrl(term)) {
                        bearerAuth(
                            oauth.issueToken(claims = withApplicationRoles(AppRoles.READ_GJENNOMFORING)).serialize(),
                        )
                    }

                    response.status shouldBe HttpStatusCode.OK

                    val responseBody = response.body<List<BrregHovedenhetDto>>()
                    responseBody.size shouldBe 1
                }
            }
        }

        context("hent underenheter") {
            val underenhetUrl = { orgnr: Organisasjonsnummer -> "/api/v1/arrangor/hovedenhet/$orgnr/underenheter" }

            test("401 når påkrevde claims mangler fra token") {
                withTestApplication(appConfig()) {
                    val orgnr = Organisasjonsnummer("123456789")
                    val response = client.get(underenhetUrl(orgnr)) {
                        bearerAuth(
                            oauth.issueToken(claims = withApplicationRoles()).serialize(),
                        )
                    }

                    response.status shouldBe HttpStatusCode.Unauthorized
                }
            }

            test("200 når søk returneres") {
                val orgnr = Organisasjonsnummer("924203617")

                val localAppConfig = appConfig().copy(
                    engine = createMockEngine {
                        get("https://data.brreg.no/enhetsregisteret/api/underenheter") {
                            respondJson(BrregFixtures.SOK_UNDERENHET)
                        }
                    },
                )
                withTestApplication(localAppConfig) {
                    val response = client.get(underenhetUrl(orgnr)) {
                        bearerAuth(
                            oauth.issueToken(claims = withApplicationRoles(AppRoles.READ_GJENNOMFORING)).serialize(),
                        )
                    }

                    response.status shouldBe HttpStatusCode.OK

                    val responseBody = response.body<List<BrregUnderenhetDto>>()
                    responseBody.size shouldBe 1
                }
            }
        }
    }
})

private fun withApplicationRoles(roles: String? = null): Map<String, List<String>> = mapOf(
    "roles" to listOfNotNull(AppRoles.ACCESS_AS_APPLICATION, roles),
)
