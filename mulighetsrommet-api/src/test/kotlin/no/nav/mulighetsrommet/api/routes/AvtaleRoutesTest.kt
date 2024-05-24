package no.nav.mulighetsrommet.api.routes

import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import no.nav.mulighetsrommet.api.*
import no.nav.mulighetsrommet.api.clients.brreg.BrregEmbeddedUnderenheter
import no.nav.mulighetsrommet.api.clients.brreg.BrregEnhet
import no.nav.mulighetsrommet.api.clients.brreg.BrregUnderenheter
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattRolle
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.dto.allowedAvtaletypes
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.util.*

class AvtaleRoutesTest : FunSpec({
    val databaseConfig = createDatabaseTestConfig()
    val database = extension(FlywayDatabaseTestListener(databaseConfig))
    val domain =
        MulighetsrommetTestDomain(enheter = listOf(NavEnhetFixtures.IT, NavEnhetFixtures.Oslo), avtaler = emptyList())
    val oauth = MockOAuth2Server()

    beforeSpec {
        oauth.start()
        domain.initialize(database.db)
    }

    afterSpec {
        oauth.shutdown()
    }

    test("401 Unauthorized for uautentisert kall for PUT av avtaledata") {
        val config = createTestApplicationConfig().copy(
            auth = createAuthConfig(oauth, roles = listOf()),
        )
        withTestApplication(config) {
            val response = client.put("/api/v1/intern/avtaler")
            response.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    test("401 Unauthorized for uautentisert kall for PUT av avtaledata når bruker ikke har tilgang til å skrive for avtaler") {
        val avtaleSkrivRolle = AdGruppeNavAnsattRolleMapping(UUID.randomUUID(), NavAnsattRolle.AVTALER_SKRIV)
        val config = createTestApplicationConfig().copy(
            auth = createAuthConfig(oauth, roles = listOf(avtaleSkrivRolle)),
        )
        withTestApplication(config) {
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
        val avtaleSkrivRolle = AdGruppeNavAnsattRolleMapping(UUID.randomUUID(), NavAnsattRolle.AVTALER_SKRIV)
        val tiltaksadministrasjonGenerellRolle =
            AdGruppeNavAnsattRolleMapping(UUID.randomUUID(), NavAnsattRolle.TILTAKADMINISTRASJON_GENERELL)
        val config = createTestApplicationConfig().copy(
            auth = createAuthConfig(oauth, roles = listOf(avtaleSkrivRolle, tiltaksadministrasjonGenerellRolle)),
        )
        withTestApplication(config) {
            val response = client.put("/api/v1/intern/avtaler") {
                val claims = mapOf(
                    "NAVident" to "ABC123",
                    "groups" to listOf(tiltaksadministrasjonGenerellRolle.adGruppeId),
                )
                bearerAuth(
                    oauth.issueToken(claims = claims).serialize(),
                )
            }
            response.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    test("Skal gi korrekt statuskode basert på om vi har tatt eierskap til tiltakstype eller ikke") {
        val avtaleSkrivRolle = AdGruppeNavAnsattRolleMapping(UUID.randomUUID(), NavAnsattRolle.AVTALER_SKRIV)
        val tiltaksadministrasjonGenerellRolle =
            AdGruppeNavAnsattRolleMapping(UUID.randomUUID(), NavAnsattRolle.TILTAKADMINISTRASJON_GENERELL)
        val engine = createMockEngine(
            "/brreg/enheter/${AvtaleFixtures.avtaleRequest.arrangorOrganisasjonsnummer}" to {
                respondJson(BrregEnhet(organisasjonsnummer = "123456789", navn = "Testvirksomhet"))
            },
            "/brreg/underenheter" to {
                respondJson(BrregEmbeddedUnderenheter(_embedded = BrregUnderenheter(underenheter = emptyList())))
            },
        )
        val config = createTestApplicationConfig().copy(
            auth = createAuthConfig(oauth, roles = listOf(avtaleSkrivRolle, tiltaksadministrasjonGenerellRolle)),
            engine = engine,
            database = databaseConfig,
            migrerteTiltak = listOf(Tiltakskode.OPPFOLGING),
        )
        withTestApplication(config) {
            val client = createClient {
                install(ContentNegotiation) {
                    json()
                }
            }

            forAll(
                row(TiltakstypeFixtures.VTA, HttpStatusCode.OK),
                row(TiltakstypeFixtures.AFT, HttpStatusCode.OK),
                row(TiltakstypeFixtures.Oppfolging, HttpStatusCode.OK),
                row(TiltakstypeFixtures.Jobbklubb, HttpStatusCode.BadRequest),
            ) { tiltakstype, status ->
                val response = client.put("/api/v1/intern/avtaler") {
                    val claims = mapOf(
                        "NAVident" to "ABC123",
                        "groups" to listOf(avtaleSkrivRolle.adGruppeId, tiltaksadministrasjonGenerellRolle.adGruppeId),
                    )
                    bearerAuth(
                        oauth.issueToken(claims = claims).serialize(),
                    )
                    contentType(ContentType.Application.Json)
                    setBody(
                        AvtaleFixtures.avtaleRequest.copy(
                            id = UUID.randomUUID(),
                            avtaletype = allowedAvtaletypes(Tiltakskode.fromArenaKode(tiltakstype.arenaKode))[0],
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
        val config = createTestApplicationConfig().copy(
            auth = createAuthConfig(oauth, roles = emptyList()),
            database = databaseConfig,
        )
        withTestApplication(config) {
            val response = client.get("/api/v1/intern/avtaler") {
                val claims = mapOf(
                    "NAVident" to "ABC123",
                )
                bearerAuth(
                    oauth.issueToken(claims = claims).serialize(),
                )
            }
            response.status shouldBe HttpStatusCode.OK
        }
    }
})
