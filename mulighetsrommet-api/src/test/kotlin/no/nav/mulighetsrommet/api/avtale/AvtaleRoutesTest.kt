package no.nav.mulighetsrommet.api.avtale

import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import no.nav.mulighetsrommet.api.*
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattRolle
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.allowedAvtaletypes
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.util.*

class AvtaleRoutesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val domain = MulighetsrommetTestDomain(
        enheter = listOf(NavEnhetFixtures.Innlandet, NavEnhetFixtures.Oslo),
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

    fun appConfig(
        engine: HttpClientEngine = CIO.create(),
    ) = createTestApplicationConfig().copy(
        database = databaseConfig,
        auth = createAuthConfig(oauth, roles = listOf(generellRolle, avtaleSkrivRolle)),
        engine = engine,
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

    test("Skal gi korrekt statuskode basert på om vi har tatt eierskap til tiltakstype eller ikke") {
        withTestApplication(appConfig()) {
            val client = createClient {
                install(ContentNegotiation) {
                    json()
                }
            }

            forAll(
                row(TiltakstypeFixtures.VTA, HttpStatusCode.OK),
                row(TiltakstypeFixtures.AFT, HttpStatusCode.OK),
                row(TiltakstypeFixtures.Oppfolging, HttpStatusCode.OK),
                row(TiltakstypeFixtures.Jobbklubb, HttpStatusCode.OK),
            ) { tiltakstype, status ->
                val response = client.put("/api/v1/intern/avtaler") {
                    val claims = mapOf(
                        "NAVident" to "ABC123",
                        "groups" to listOf(avtaleSkrivRolle.adGruppeId, generellRolle.adGruppeId),
                    )
                    bearerAuth(
                        oauth.issueToken(claims = claims).serialize(),
                    )
                    contentType(ContentType.Application.Json)
                    setBody(
                        AvtaleFixtures.avtaleRequest.copy(
                            id = UUID.randomUUID(),
                            avtaletype = allowedAvtaletypes(tiltakstype.tiltakskode).first(),
                            navEnheter = listOf(NavEnhetFixtures.Oslo.enhetsnummer),
                            tiltakstypeId = tiltakstype.id,
                        ),
                    )
                }
                response.status shouldBe status
            }
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
