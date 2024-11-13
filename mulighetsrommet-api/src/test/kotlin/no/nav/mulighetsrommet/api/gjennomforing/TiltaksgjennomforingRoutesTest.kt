package no.nav.mulighetsrommet.api.gjennomforing

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
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattRolle
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.util.*

class TiltaksgjennomforingRoutesTest : FunSpec({
    val databaseConfig = databaseConfig
    val database = extension(FlywayDatabaseTestListener(databaseConfig))
    val domain = MulighetsrommetTestDomain(
        enheter = listOf(NavEnhetFixtures.IT, NavEnhetFixtures.Oslo, NavEnhetFixtures.Sagene),
        arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
        avtaler = listOf(
            AvtaleFixtures.oppfolging.copy(
                navEnheter = listOf(
                    NavEnhetFixtures.Sagene.enhetsnummer,
                    NavEnhetFixtures.Oslo.enhetsnummer,
                ),
                arrangorUnderenheter = listOf(ArrangorFixtures.underenhet1.id),
            ),
            AvtaleFixtures.VTA.copy(
                navEnheter = listOf(
                    NavEnhetFixtures.Sagene.enhetsnummer,
                    NavEnhetFixtures.Oslo.enhetsnummer,
                ),
                arrangorUnderenheter = listOf(ArrangorFixtures.underenhet1.id),
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

    val generellRolle = AdGruppeNavAnsattRolleMapping(UUID.randomUUID(), NavAnsattRolle.TILTAKADMINISTRASJON_GENERELL)
    val gjennomforingerSkriv =
        AdGruppeNavAnsattRolleMapping(UUID.randomUUID(), NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV)

    fun appConfig(
        engine: HttpClientEngine = CIO.create(),
    ) = createTestApplicationConfig().copy(
        database = databaseConfig,
        auth = createAuthConfig(oauth, roles = listOf(generellRolle, gjennomforingerSkriv)),
        engine = engine,
    )

    test("401 Unauthorized for uautentisert kall for PUT av tiltaksgjennomføring") {
        withTestApplication(appConfig()) {
            val response = client.put("/api/v1/intern/tiltaksgjennomforinger")
            response.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    test("401 Unauthorized for uautentisert kall for PUT av tiltaksgjennomføring når bruker ikke har tilgang til å skrive for tiltaksgjennomføringer") {
        withTestApplication(appConfig()) {
            val response = client.put("/api/v1/intern/tiltaksgjennomforinger") {
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
        withTestApplication(appConfig()) {
            val response = client.put("/api/v1/intern/tiltaksgjennomforinger") {
                val claims = mapOf(
                    "NAVident" to "ABC123",
                    "groups" to listOf(gjennomforingerSkriv.adGruppeId),
                )
                bearerAuth(
                    oauth.issueToken(claims = claims).serialize(),
                )
            }
            response.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    test("200 OK for autentisert kall for PUT av tiltaksgjennomføring når bruker har generell tilgang og til skriv for tiltaksgjennomføring") {
        withTestApplication(appConfig()) {
            val client = createClient {
                install(ContentNegotiation) {
                    json()
                }
            }

            forAll(
                row(AvtaleFixtures.oppfolging, HttpStatusCode.OK),
                row(AvtaleFixtures.VTA, HttpStatusCode.BadRequest),
            ) { avtale, status ->
                val response = client.put("/api/v1/intern/tiltaksgjennomforinger") {
                    val claims = mapOf(
                        "NAVident" to "ABC123",
                        "groups" to listOf(generellRolle.adGruppeId, gjennomforingerSkriv.adGruppeId),
                    )
                    bearerAuth(
                        oauth.issueToken(claims = claims).serialize(),
                    )
                    contentType(ContentType.Application.Json)
                    setBody(
                        TiltaksgjennomforingFixtures.Oppfolging1Request.copy(
                            avtaleId = avtale.id,
                            navRegion = NavEnhetFixtures.Oslo.enhetsnummer,
                            navEnheter = listOf(NavEnhetFixtures.Sagene.enhetsnummer),
                            tiltakstypeId = avtale.tiltakstypeId,
                        ),
                    )
                }
                response.status shouldBe status
            }
        }
    }

    test("200 OK for autentisert kall for GET av tiltaksgjennomføringer") {
        withTestApplication(appConfig()) {
            val response = client.get("/api/v1/intern/tiltaksgjennomforinger") {
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
