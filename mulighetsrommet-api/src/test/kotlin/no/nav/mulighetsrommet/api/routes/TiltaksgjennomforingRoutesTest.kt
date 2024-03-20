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
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattRolle
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.util.*

class TiltaksgjennomforingRoutesTest : FunSpec({
    val databaseConfig = createDatabaseTestConfig()
    val database = extension(FlywayDatabaseTestListener(databaseConfig))
    val domain = MulighetsrommetTestDomain(
        enheter = listOf(NavEnhetFixtures.IT, NavEnhetFixtures.Oslo, NavEnhetFixtures.Sagene),
        virksomheter = listOf(VirksomhetFixtures.hovedenhet, VirksomhetFixtures.underenhet1),
        avtaler = listOf(
            AvtaleFixtures.oppfolging.copy(
                navEnheter = listOf(
                    NavEnhetFixtures.Sagene.enhetsnummer,
                    NavEnhetFixtures.Oslo.enhetsnummer,
                ),
                arrangorUnderenheter = listOf(VirksomhetFixtures.underenhet1.id),
            ),
            AvtaleFixtures.VTA.copy(
                navEnheter = listOf(
                    NavEnhetFixtures.Sagene.enhetsnummer,
                    NavEnhetFixtures.Oslo.enhetsnummer,
                ),
                arrangorUnderenheter = listOf(VirksomhetFixtures.underenhet1.id),
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
            auth = createAuthConfig(
                oauth,
                roles = listOf(tiltaksgjennomforingSkrivRolle, tiltaksadministrasjonGenerellRolle),
            ),
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
        val engine = createMockEngine()
        val config = createTestApplicationConfig().copy(
            auth = createAuthConfig(
                oauth,
                roles = listOf(tiltaksgjennomforingSkrivRolle, tiltaksadministrasjonGenerellRolle),
            ),
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
                row(TiltakstypeFixtures.Oppfolging, HttpStatusCode.OK),
                row(TiltakstypeFixtures.VTA, HttpStatusCode.BadRequest),
            ) { tiltakstype, status ->
                val response = client.put("/api/v1/internal/tiltaksgjennomforinger") {
                    val claims = mapOf(
                        "NAVident" to "ABC123",
                        "groups" to listOf(
                            tiltaksgjennomforingSkrivRolle.adGruppeId,
                            tiltaksadministrasjonGenerellRolle.adGruppeId,
                        ),
                    )
                    bearerAuth(
                        oauth.issueToken(claims = claims).serialize(),
                    )
                    contentType(ContentType.Application.Json)
                    setBody(
                        TiltaksgjennomforingFixtures.Oppfolging1Request.copy(
                            avtaleId = AvtaleFixtures.oppfolging.id,
                            navRegion = NavEnhetFixtures.Oslo.enhetsnummer,
                            navEnheter = listOf(NavEnhetFixtures.Sagene.enhetsnummer),
                            tiltakstypeId = tiltakstype.id,
                        ),
                    )
                }
                response.status shouldBe status
            }
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
