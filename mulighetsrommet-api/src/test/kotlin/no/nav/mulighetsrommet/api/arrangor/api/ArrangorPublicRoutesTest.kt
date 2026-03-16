package no.nav.mulighetsrommet.api.arrangor.api

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAnyOf
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.QueryContext
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
import org.intellij.lang.annotations.Language

class ArrangorPublicRoutesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val oauth = MockOAuth2Server()
    val utenlandskArrangor =
        ArrangorFixtures.Utenlandsk.hovedenhet

    val domain = MulighetsrommetTestDomain(
        navEnheter = listOf(NavEnhetFixtures.Innlandet),
        ansatte = listOf(NavAnsattFixture.DonaldDuck),
        arrangorer = listOf(
            ArrangorFixtures.hovedenhet,
            ArrangorFixtures.underenhet1,
            utenlandskArrangor,
        ),
        avtaler = emptyList(),
        gjennomforinger = emptyList(),
    )

    beforeSpec {
        oauth.start()
    }

    beforeEach {
        domain.initialize(database.db)
        database.db.session {
            setUtenlandskArrangor(utenlandskArrangor.organisasjonsnummer)
        }
    }

    afterEach {
        database.truncateAll()
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
                        get("https://data.brreg.no/enhetsregisteret/api/enheter") {
                            respondJson(BrregFixtures.SOK_ENHET)
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

                    val responseBody = response.body<List<BrregHovedenhetDto>>()
                    responseBody.size shouldBe 1
                }
            }

            test("200 tom liste ved tomt resultat") {
                val localAppConfig = appConfig().copy(
                    engine = createMockEngine {
                        get("https://data.brreg.no/enhetsregisteret/api/enheter") {
                            respondJson(BrregFixtures.SOK_ENHET_INGEN_TREFF)
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

                    val responseBody = response.body<List<BrregHovedenhetDto>>()
                    responseBody.size shouldBe 0
                }
            }

            test("200 inkluder utenlandske arrangører i søket") {
                val localAppConfig = appConfig().copy(
                    engine = createMockEngine {
                        get("https://data.brreg.no/enhetsregisteret/api/enheter") {
                            respondJson(BrregFixtures.SOK_ENHET)
                        }
                    },
                )
                withTestApplication(localAppConfig) {
                    val term = "utenlandsk"
                    val response = client.get(sokUrl(term)) {
                        bearerAuth(
                            oauth.issueToken(claims = withApplicationRoles(AppRoles.READ_GJENNOMFORING)).serialize(),
                        )
                    }

                    response.status shouldBe HttpStatusCode.OK

                    val responseBody = response.body<List<BrregHovedenhetDto>>()
                    responseBody.size shouldBe 2
                    responseBody.map { it.organisasjonsnummer }.shouldContainAnyOf(utenlandskArrangor.organisasjonsnummer)
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

            test("200 ved treff på søk") {
                val localAppConfig = appConfig().copy(
                    engine = createMockEngine {
                        get("https://data.brreg.no/enhetsregisteret/api/underenheter") {
                            respondJson(BrregFixtures.SOK_UNDERENHET)
                        }
                    },
                )

                val orgnr = Organisasjonsnummer("924203618")
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

            test("200 tom liste hvis ingen treff på søk") {
                val localAppConfig = appConfig().copy(
                    engine = createMockEngine {
                        get("https://data.brreg.no/enhetsregisteret/api/underenheter") {
                            respondJson(BrregFixtures.SOK_UNDERENHET_INGEN_TREFF)
                        }
                    },
                )

                val orgnr = Organisasjonsnummer("924203618")
                withTestApplication(localAppConfig) {
                    val response = client.get(underenhetUrl(orgnr)) {
                        bearerAuth(
                            oauth.issueToken(claims = withApplicationRoles(AppRoles.READ_GJENNOMFORING)).serialize(),
                        )
                    }

                    response.status shouldBe HttpStatusCode.OK

                    val responseBody = response.body<List<BrregUnderenhetDto>>()
                    responseBody.size shouldBe 0
                }
            }

            test("returnerer bare den utenlandske arranøren om orgnr er det samme") {
                withTestApplication(appConfig()) {
                    val response = client.get(underenhetUrl(utenlandskArrangor.organisasjonsnummer)) {
                        bearerAuth(
                            oauth.issueToken(claims = withApplicationRoles(AppRoles.READ_GJENNOMFORING)).serialize(),
                        )
                    }

                    response.status shouldBe HttpStatusCode.OK

                    val responseBody = response.body<List<BrregUnderenhetDto>>()
                    responseBody.size shouldBe 1
                    responseBody[0].organisasjonsnummer shouldBe utenlandskArrangor.organisasjonsnummer
                }
            }
        }
    }
})

private fun withApplicationRoles(roles: String? = null): Map<String, List<String>> = mapOf(
    "roles" to listOfNotNull(AppRoles.ACCESS_AS_APPLICATION, roles),
)

private fun QueryContext.setUtenlandskArrangor(organisasjonsnummer: Organisasjonsnummer) {
    @Language("PostgreSQL")
    val query = """
            update arrangor
            set er_utenlandsk_virksomhet = true
            where organisasjonsnummer = ?
    """.trimIndent()

    session.execute(queryOf(query, organisasjonsnummer.value))
}
