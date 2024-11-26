package no.nav.mulighetsrommet.api.gjennomforing

import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import no.nav.mulighetsrommet.api.*
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.gjennomforing.db.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattRolle
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.domain.dto.AvbruttAarsak
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingStatus
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class TiltaksgjennomforingRoutesTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))
    val oauth = MockOAuth2Server()

    beforeSpec {
        oauth.start()
    }

    afterSpec {
        oauth.shutdown()
    }

    val generellRolle = AdGruppeNavAnsattRolleMapping(
        UUID.randomUUID(),
        NavAnsattRolle.TILTAKADMINISTRASJON_GENERELL,
    )
    val gjennomforingerSkriv = AdGruppeNavAnsattRolleMapping(
        UUID.randomUUID(),
        NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV,
    )

    fun appConfig(
        engine: HttpClientEngine = CIO.create(),
    ) = createTestApplicationConfig().copy(
        auth = createAuthConfig(oauth, roles = listOf(generellRolle, gjennomforingerSkriv)),
        engine = engine,
    )

    context("opprett og les gjennomføring") {
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

        beforeAny {
            domain.initialize(database.db)
        }

        afterContainer {
            database.db.truncateAll()
        }

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
                ) { avtale, expectedStatus ->
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
                    response.status shouldBe expectedStatus
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
    }

    context("avbryt gjennomføring") {
        val domain2 = MulighetsrommetTestDomain(
            avtaler = listOf(AvtaleFixtures.oppfolging),
            gjennomforinger = listOf(
                TiltaksgjennomforingFixtures.Oppfolging1.copy(
                    startDato = LocalDate.now(),
                    sluttDato = LocalDate.now(),
                ),
                TiltaksgjennomforingFixtures.Oppfolging1.copy(
                    id = UUID.randomUUID(),
                ),
            ),
        )

        beforeAny {
            val gjennomforinger = TiltaksgjennomforingRepository(database.db)

            domain2.initialize(database.db)

            gjennomforinger.avbryt(
                domain2.gjennomforinger[1].id,
                LocalDateTime.now(),
                AvbruttAarsak.Feilregistrering,
            )
        }

        afterContainer {
            database.db.truncateAll()
        }

        test("not found når gjennomføring ikke finnes") {
            withTestApplication(appConfig()) {
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val response = client
                    .put("/api/v1/intern/tiltaksgjennomforinger/${UUID.randomUUID()}/avbryt") {
                        val claims = mapOf(
                            "NAVident" to "ABC123",
                            "groups" to listOf(generellRolle.adGruppeId, gjennomforingerSkriv.adGruppeId),
                        )
                        bearerAuth(oauth.issueToken(claims = claims).serialize())
                        contentType(ContentType.Application.Json)
                        setBody(AvbrytRequest(aarsak = null))
                    }

                response.status shouldBe HttpStatusCode.NotFound
                response.bodyAsText().shouldBe("Gjennomføringen finnes ikke")
            }
        }

        test("bad request når årsak mangler") {
            withTestApplication(appConfig()) {
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val response = client
                    .put("/api/v1/intern/tiltaksgjennomforinger/${domain2.gjennomforinger[0].id}/avbryt") {
                        val claims = mapOf(
                            "NAVident" to "ABC123",
                            "groups" to listOf(generellRolle.adGruppeId, gjennomforingerSkriv.adGruppeId),
                        )
                        bearerAuth(oauth.issueToken(claims = claims).serialize())
                        contentType(ContentType.Application.Json)
                        setBody(AvbrytRequest(aarsak = null))
                    }

                response.status shouldBe HttpStatusCode.BadRequest
                response.bodyAsText().shouldBe("Årsak mangler")
            }
        }

        test("bad request når beskrivelse mangler for Annen årsak") {
            withTestApplication(appConfig()) {
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val response = client
                    .put("/api/v1/intern/tiltaksgjennomforinger/${domain2.gjennomforinger[0].id}/avbryt") {
                        val claims = mapOf(
                            "NAVident" to "ABC123",
                            "groups" to listOf(generellRolle.adGruppeId, gjennomforingerSkriv.adGruppeId),
                        )
                        bearerAuth(oauth.issueToken(claims = claims).serialize())
                        contentType(ContentType.Application.Json)
                        setBody(AvbrytRequest(aarsak = AvbruttAarsak.Annet("")))
                    }

                response.status shouldBe HttpStatusCode.BadRequest
                response.bodyAsText().shouldBe("Beskrivelse er obligatorisk når “Annet” er valgt som årsak")
            }
        }

        test("bad request når gjennomføring allerede er avsluttet") {
            withTestApplication(appConfig()) {
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val response = client
                    .put("/api/v1/intern/tiltaksgjennomforinger/${domain2.gjennomforinger[1].id}/avbryt") {
                        val claims = mapOf(
                            "NAVident" to "ABC123",
                            "groups" to listOf(generellRolle.adGruppeId, gjennomforingerSkriv.adGruppeId),
                        )
                        bearerAuth(oauth.issueToken(claims = claims).serialize())
                        contentType(ContentType.Application.Json)
                        setBody(AvbrytRequest(aarsak = AvbruttAarsak.Feilregistrering))
                    }

                response.status shouldBe HttpStatusCode.BadRequest
                response.bodyAsText().shouldBe("Gjennomføringen er allerede avsluttet og kan derfor ikke avbrytes.")
            }
        }

        test("avbryter gjennomføring") {
            val gjennomforinger = TiltaksgjennomforingRepository(database.db)

            withTestApplication(appConfig()) {
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val response = client
                    .put("/api/v1/intern/tiltaksgjennomforinger/${domain2.gjennomforinger[0].id}/avbryt") {
                        val claims = mapOf(
                            "NAVident" to "ABC123",
                            "groups" to listOf(generellRolle.adGruppeId, gjennomforingerSkriv.adGruppeId),
                        )
                        bearerAuth(oauth.issueToken(claims = claims).serialize())
                        contentType(ContentType.Application.Json)
                        setBody(AvbrytRequest(aarsak = AvbruttAarsak.Feilregistrering))
                    }

                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText().shouldBeEmpty()

                gjennomforinger.get(domain2.gjennomforinger[0].id).shouldNotBeNull().should {
                    it.status.status shouldBe TiltaksgjennomforingStatus.AVBRUTT
                    it.status.avbrutt?.aarsak shouldBe AvbruttAarsak.Feilregistrering
                }
            }
        }
    }
})
