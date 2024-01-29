package no.nav.mulighetsrommet.api.routes

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import no.nav.mulighetsrommet.api.*
import no.nav.mulighetsrommet.api.clients.brreg.BrregEmbeddedUnderenheter
import no.nav.mulighetsrommet.api.clients.brreg.BrregEnhet
import no.nav.mulighetsrommet.api.clients.brreg.BrregUnderenheter
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattRolle
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.util.*

class TiltaksgjennomforingRoutesTest : FunSpec({
    val databaseConfig = createDatabaseTestConfig()
    val database = extension(FlywayDatabaseTestListener(databaseConfig))
    val domain =
        MulighetsrommetTestDomain(
            enheter = listOf(NavEnhetFixtures.IT, NavEnhetFixtures.Oslo, NavEnhetFixtures.Sagene),
            avtaler = listOf(
                AvtaleFixtures.avtale1.copy(
                    navEnheter = listOf(
                        NavEnhetFixtures.Sagene.enhetsnummer,
                        NavEnhetFixtures.Oslo.enhetsnummer,
                    ),
                    leverandorUnderenheter = listOf(TiltaksgjennomforingFixtures.Oppfolging1.arrangorOrganisasjonsnummer),
                ),
                AvtaleFixtures.avtaleForVta.copy(
                    navEnheter = listOf(
                        NavEnhetFixtures.Sagene.enhetsnummer,
                        NavEnhetFixtures.Oslo.enhetsnummer,
                    ),
                    leverandorUnderenheter = listOf(TiltaksgjennomforingFixtures.Oppfolging1.arrangorOrganisasjonsnummer),
                ),
            ),
        )
    val oauth = MockOAuth2Server()

    beforeSpec {
        oauth.start()
        domain.initialize(database.db)
    }

    afterSpec {
        oauth.shutdown()
    }

    test("401 Unauthorized for uautentisert kall for PUT av tiltaksgjennomføring") {
        val config = createTestApplicationConfig().copy(
            auth = createAuthConfig(oauth, roles = listOf()),
        )
        withTestApplication(config) {
            val response = client.put("/api/v1/internal/tiltaksgjennomforinger")
            response.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    test("401 Unauthorized for uautentisert kall for PUT av tiltaksgjennomføring når bruker ikke har tilgang til å skrive for tiltaksgjennomføringer") {
        val tiltaksgjennomforingSkrivRolle =
            AdGruppeNavAnsattRolleMapping(UUID.randomUUID(), NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV)
        val config = createTestApplicationConfig().copy(
            auth = createAuthConfig(oauth, roles = listOf(tiltaksgjennomforingSkrivRolle)),
        )
        withTestApplication(config) {
            val response = client.put("/api/v1/internal/tiltaksgjennomforinger") {
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

    test("401 Unauthorized for uautentisert kall for PUT av tiltaksgjennomføring når bruker har tilgang til å skrive for tiltaksgjennomføringer, men mangler generell tilgang") {
        val tiltaksgjennomforingSkrivRolle =
            AdGruppeNavAnsattRolleMapping(UUID.randomUUID(), NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV)
        val tiltaksadministrasjonGenerellRolle =
            AdGruppeNavAnsattRolleMapping(UUID.randomUUID(), NavAnsattRolle.TILTAKADMINISTRASJON_GENERELL)
        val config = createTestApplicationConfig().copy(
            auth = createAuthConfig(oauth, roles = listOf(tiltaksgjennomforingSkrivRolle, tiltaksadministrasjonGenerellRolle)),
        )
        withTestApplication(config) {
            val response = client.put("/api/v1/internal/tiltaksgjennomforinger") {
                val claims = mapOf(
                    "NAVident" to "ABC123",
                    "groups" to listOf(tiltaksgjennomforingSkrivRolle.adGruppeId),
                )
                bearerAuth(
                    oauth.issueToken(claims = claims).serialize(),
                )
            }
            response.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    test("200 OK for autentisert kall for PUT av tiltaksgjennomføring når bruker har generell tilgang og til skriv for tiltaksgjennomføring") {
        val tiltaksgjennomforingSkrivRolle =
            AdGruppeNavAnsattRolleMapping(UUID.randomUUID(), NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV)
        val tiltaksadministrasjonGenerellRolle =
            AdGruppeNavAnsattRolleMapping(UUID.randomUUID(), NavAnsattRolle.TILTAKADMINISTRASJON_GENERELL)
        val engine = createMockEngine(
            "/brreg/enheter/${TiltaksgjennomforingFixtures.Oppfolging1Request.arrangorOrganisasjonsnummer}" to {
                respondJson(BrregEnhet(organisasjonsnummer = "123456789", navn = "Testvirksomhet"))
            },
            "/brreg/underenheter" to {
                respondJson(BrregEmbeddedUnderenheter(_embedded = BrregUnderenheter(underenheter = emptyList())))
            },
        )
        val config = createTestApplicationConfig().copy(
            auth = createAuthConfig(oauth, roles = listOf(tiltaksgjennomforingSkrivRolle, tiltaksadministrasjonGenerellRolle)),
            engine = engine,
            database = databaseConfig,
        )
        withTestApplication(config) {
            val client = createClient {
                install(ContentNegotiation) {
                    json()
                }
            }
            val response = client.put("/api/v1/internal/tiltaksgjennomforinger") {
                val claims = mapOf(
                    "NAVident" to "ABC123",
                    "groups" to listOf(tiltaksgjennomforingSkrivRolle.adGruppeId, tiltaksadministrasjonGenerellRolle.adGruppeId),
                )
                bearerAuth(
                    oauth.issueToken(claims = claims).serialize(),
                )
                contentType(ContentType.Application.Json)
                setBody(
                    TiltaksgjennomforingFixtures.Oppfolging1Request.copy(
                        avtaleId = AvtaleFixtures.avtale1.id,
                        navRegion = NavEnhetFixtures.Oslo.enhetsnummer,
                        navEnheter = listOf(NavEnhetFixtures.Sagene.enhetsnummer),
                        tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                    ),
                )
            }
            response.status shouldBe HttpStatusCode.OK
        }
    }

    test("400 Bad request for autentisert kall for PUT av tiltaksgjennomføring når tiltakstypen ikke er skrudd på som master") {
        val tiltaksgjennomforingSkrivRolle =
            AdGruppeNavAnsattRolleMapping(UUID.randomUUID(), NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV)
        val tiltaksadministrasjonGenerellRolle =
            AdGruppeNavAnsattRolleMapping(UUID.randomUUID(), NavAnsattRolle.TILTAKADMINISTRASJON_GENERELL)
        val engine = createMockEngine(
            "/brreg/enheter/${TiltaksgjennomforingFixtures.Oppfolging1Request.arrangorOrganisasjonsnummer}" to {
                respondJson(BrregEnhet(organisasjonsnummer = "123456789", navn = "Testvirksomhet"))
            },
            "/brreg/underenheter" to {
                respondJson(BrregEmbeddedUnderenheter(_embedded = BrregUnderenheter(underenheter = emptyList())))
            },
        )
        val config = createTestApplicationConfig().copy(
            auth = createAuthConfig(oauth, roles = listOf(tiltaksgjennomforingSkrivRolle, tiltaksadministrasjonGenerellRolle)),
            engine = engine,
            database = databaseConfig,
        )
        withTestApplication(config) {
            val client = createClient {
                install(ContentNegotiation) {
                    json()
                }
            }
            val response = client.put("/api/v1/internal/tiltaksgjennomforinger") {
                val claims = mapOf(
                    "NAVident" to "ABC123",
                    "groups" to listOf(tiltaksgjennomforingSkrivRolle.adGruppeId, tiltaksadministrasjonGenerellRolle.adGruppeId),
                )
                bearerAuth(
                    oauth.issueToken(claims = claims).serialize(),
                )
                contentType(ContentType.Application.Json)
                setBody(
                    TiltaksgjennomforingFixtures.Oppfolging1Request.copy(
                        avtaleId = AvtaleFixtures.avtaleForVta.id,
                        navRegion = NavEnhetFixtures.Oslo.enhetsnummer,
                        navEnheter = listOf(NavEnhetFixtures.Sagene.enhetsnummer),
                        tiltakstypeId = TiltakstypeFixtures.VTA.id,
                    ),
                )
            }
            response.status shouldBe HttpStatusCode.BadRequest
        }
    }

    test("200 OK for autentisert kall for GET av tiltaksgjennomføringer") {
        val config = createTestApplicationConfig().copy(
            auth = createAuthConfig(oauth, roles = emptyList()),
            database = databaseConfig,
        )
        withTestApplication(config) {
            val response = client.get("/api/v1/internal/tiltaksgjennomforinger") {
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
