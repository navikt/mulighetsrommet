package no.nav.mulighetsrommet.api.gjennomforing

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import io.kotest.matchers.types.shouldBeTypeOf
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import no.nav.mulighetsrommet.api.*
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingStatusDto
import no.nav.mulighetsrommet.api.navansatt.ktor.NavAnsattManglerTilgang
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.AvbruttAarsak
import no.nav.mulighetsrommet.model.GjennomforingStatus
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class GjennomforingRoutesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val oauth = MockOAuth2Server()

    beforeSpec {
        oauth.start()
    }

    afterSpec {
        oauth.shutdown()
    }

    val generellRolle = EntraGroupNavAnsattRolleMapping(UUID.randomUUID(), Rolle.TILTAKADMINISTRASJON_GENERELL)
    val gjennomforingSkrivRolle = EntraGroupNavAnsattRolleMapping(UUID.randomUUID(), Rolle.TILTAKSGJENNOMFORINGER_SKRIV)

    fun appConfig() = createTestApplicationConfig().copy(
        auth = createAuthConfig(oauth, roles = setOf(generellRolle, gjennomforingSkrivRolle)),
    )

    val ansatt = NavAnsattFixture.DonaldDuck

    context("les gjennomføringer") {
        val domain = MulighetsrommetTestDomain(
            navEnheter = listOf(NavEnhetFixtures.Innlandet, NavEnhetFixtures.Oslo),
            ansatte = listOf(ansatt),
            arrangorer = listOf(),
            avtaler = listOf(),
            gjennomforinger = listOf(),
        )

        beforeAny {
            domain.initialize(database.db)
        }

        afterContainer {
            database.truncateAll()
        }

        test("401 Unauthorized for uautentisert kall") {
            withTestApplication(appConfig()) {
                val response = client.get("/api/v1/intern/gjennomforinger")
                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("403 Forbidden når bruker ikke har generell tilgang") {
            withTestApplication(appConfig()) {
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val navAnsattClaims = getAnsattClaims(ansatt, setOf(gjennomforingSkrivRolle))

                val response = client.get("/api/v1/intern/gjennomforinger") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                }

                response.status shouldBe HttpStatusCode.Forbidden
                response.body<NavAnsattManglerTilgang>().missingRoles shouldBe setOf(Rolle.TILTAKADMINISTRASJON_GENERELL)
            }
        }

        test("200 OK med generell tilgang") {
            withTestApplication(appConfig()) {
                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle))

                val response = client.get("/api/v1/intern/gjennomforinger") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                }

                response.status shouldBe HttpStatusCode.OK
            }
        }
    }

    context("opprett gjennomføring") {
        val domain = MulighetsrommetTestDomain(
            navEnheter = listOf(NavEnhetFixtures.Innlandet, NavEnhetFixtures.Oslo, NavEnhetFixtures.Sagene),
            ansatte = listOf(ansatt),
            arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
            avtaler = listOf(
                AvtaleFixtures.oppfolging.copy(
                    navEnheter = listOf(
                        NavEnhetFixtures.Sagene.enhetsnummer,
                        NavEnhetFixtures.Oslo.enhetsnummer,
                    ),
                    arrangor = AvtaleFixtures.oppfolging.arrangor?.copy(
                        underenheter = listOf(ArrangorFixtures.underenhet1.id),
                    ),
                ),
            ),
        )

        beforeAny {
            domain.initialize(database.db)
        }

        afterContainer {
            database.truncateAll()
        }

        test("403 Forbidden når bruker mangler generell tilgang") {
            withTestApplication(appConfig()) {
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val navAnsattClaims = getAnsattClaims(ansatt, setOf(gjennomforingSkrivRolle))

                val response = client.put("/api/v1/intern/gjennomforinger") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                    contentType(ContentType.Application.Json)
                    setBody("""{}""")
                }

                response.status shouldBe HttpStatusCode.Forbidden
                response.body<NavAnsattManglerTilgang>().missingRoles shouldBe setOf(Rolle.TILTAKADMINISTRASJON_GENERELL)
            }
        }

        test("403 Forbidden når bruker ikke har skrivetilgang til gjennomføringer") {
            withTestApplication(appConfig()) {
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle))

                val response = client.put("/api/v1/intern/gjennomforinger") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                }

                response.status shouldBe HttpStatusCode.Forbidden
                response.body<NavAnsattManglerTilgang>().missingRoles shouldBe setOf(Rolle.TILTAKSGJENNOMFORINGER_SKRIV)
            }
        }

        test("400 Bad Request når gjennomføringen er ugyldig") {
            withTestApplication(appConfig()) {
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle, gjennomforingSkrivRolle))
                val avtale = AvtaleFixtures.VTA

                val response = client.put("/api/v1/intern/gjennomforinger") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(
                        GjennomforingFixtures.Oppfolging1Request.copy(
                            avtaleId = avtale.id,
                            navEnheter = setOf(NavEnhetFixtures.Oslo.enhetsnummer),
                            tiltakstypeId = avtale.tiltakstypeId,
                        ),
                    )
                }

                response.status shouldBe HttpStatusCode.BadRequest
                response.body<ValidationError>().errors shouldBe listOf(FieldError("/avtaleId", "Avtalen finnes ikke"))
            }
        }

        test("200 OK når bruker har skrivetilgang til gjennomføringer") {
            withTestApplication(appConfig()) {
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle, gjennomforingSkrivRolle))
                val avtale = AvtaleFixtures.oppfolging

                val response = client.put("/api/v1/intern/gjennomforinger") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(
                        GjennomforingFixtures.Oppfolging1Request.copy(
                            avtaleId = avtale.id,
                            navEnheter = setOf(NavEnhetFixtures.Oslo.enhetsnummer),
                            tiltakstypeId = avtale.tiltakstypeId,
                        ),
                    )
                }

                response.status shouldBe HttpStatusCode.OK
            }
        }
    }

    context("endre avtale for gjennomføring") {
        val domain = MulighetsrommetTestDomain(
            ansatte = listOf(ansatt),
            avtaler = listOf(
                AvtaleFixtures.oppfolging,
                AvtaleFixtures.AFT,
                AvtaleFixtures.AFT.copy(id = UUID.randomUUID()),
            ),
            gjennomforinger = listOf(
                GjennomforingFixtures.Oppfolging1.copy(
                    startDato = LocalDate.now(),
                    sluttDato = LocalDate.now(),
                ),
                GjennomforingFixtures.AFT1.copy(
                    startDato = LocalDate.now(),
                    sluttDato = LocalDate.now(),
                ),
            ),
        )

        beforeAny {
            domain.initialize(database.db)
        }

        afterContainer {
            database.truncateAll()
        }

        test("får ikke endre avtale på anskaffede tiltak") {
            withTestApplication(appConfig()) {
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle, gjennomforingSkrivRolle))

                val response = client
                    .put("/api/v1/intern/gjennomforinger/${domain.gjennomforinger[0].id}/avtale") {
                        bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                        contentType(ContentType.Application.Json)
                        setBody(SetAvtaleForGjennomforingRequest(domain.avtaler[1].id))
                    }

                response.status shouldBe HttpStatusCode.BadRequest
                response.bodyAsText() shouldBe "Avtale kan bare settes for gjennomføringer av type AFT eller VTA"
            }
        }

        test("kan sette avtale for anskaffede tiltak") {
            withTestApplication(appConfig()) {
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle, gjennomforingSkrivRolle))

                val response = client
                    .put("/api/v1/intern/gjennomforinger/${domain.gjennomforinger[1].id}/avtale") {
                        bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                        contentType(ContentType.Application.Json)
                        setBody(SetAvtaleForGjennomforingRequest(domain.avtaler[2].id))
                    }

                response.status shouldBe HttpStatusCode.OK
            }
        }
    }

    context("avbryt gjennomføring") {
        val aktivGjennomforingId = UUID.randomUUID()
        val avbruttGjennomforingId = UUID.randomUUID()

        val domain = MulighetsrommetTestDomain(
            ansatte = listOf(ansatt),
            avtaler = listOf(AvtaleFixtures.oppfolging),
            gjennomforinger = listOf(
                GjennomforingFixtures.Oppfolging1.copy(
                    id = aktivGjennomforingId,
                    startDato = LocalDate.now(),
                    sluttDato = LocalDate.now(),
                    status = GjennomforingStatus.GJENNOMFORES,
                ),
                GjennomforingFixtures.Oppfolging1.copy(
                    id = avbruttGjennomforingId,
                ),
            ),
        ) {
            queries.gjennomforing.setStatus(
                avbruttGjennomforingId,
                GjennomforingStatus.AVBRUTT,
                LocalDateTime.now(),
                AvbruttAarsak.Feilregistrering,
            )
        }

        beforeAny {
            domain.initialize(database.db)
        }

        afterContainer {
            database.truncateAll()
        }

        test("bad request når årsak mangler") {
            withTestApplication(appConfig()) {
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle, gjennomforingSkrivRolle))

                val response = client
                    .put("/api/v1/intern/gjennomforinger/$aktivGjennomforingId/avbryt") {
                        bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                        contentType(ContentType.Application.Json)
                        setBody("{}")
                    }

                response.status shouldBe HttpStatusCode.BadRequest
            }
        }

        test("bad request når beskrivelse mangler for Annen årsak") {
            withTestApplication(appConfig()) {
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle, gjennomforingSkrivRolle))

                val response = client
                    .put("/api/v1/intern/gjennomforinger/$aktivGjennomforingId/avbryt") {
                        bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                        contentType(ContentType.Application.Json)
                        setBody(AvbrytRequest(aarsak = AvbruttAarsak.Annet("")))
                    }

                response.status shouldBe HttpStatusCode.BadRequest
                response.bodyAsText().shouldBe("Beskrivelse er obligatorisk når “Annet” er valgt som årsak")
            }
        }

        test("bad request når gjennomføring allerede er avbrutt") {
            withTestApplication(appConfig()) {
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle, gjennomforingSkrivRolle))

                val response = client
                    .put("/api/v1/intern/gjennomforinger/$avbruttGjennomforingId/avbryt") {
                        bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                        contentType(ContentType.Application.Json)
                        setBody(AvbrytRequest(aarsak = AvbruttAarsak.Feilregistrering))
                    }

                response.status shouldBe HttpStatusCode.BadRequest
                response.body<ValidationError>().errors shouldBe listOf(
                    FieldError("/", "Gjennomføringen er allerede avbrutt"),
                )
            }
        }

        test("avbryter gjennomføring") {
            withTestApplication(appConfig()) {
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle, gjennomforingSkrivRolle))

                val response = client.put("/api/v1/intern/gjennomforinger/$aktivGjennomforingId/avbryt") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(AvbrytRequest(aarsak = AvbruttAarsak.Feilregistrering))
                }

                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText().shouldBeEmpty()

                database.run {
                    queries.gjennomforing.get(aktivGjennomforingId).shouldNotBeNull().should {
                        it.status.shouldBeTypeOf<GjennomforingStatusDto.Avbrutt>().should {
                            it.type shouldBe GjennomforingStatus.AVBRUTT
                            it.aarsak shouldBe AvbruttAarsak.Feilregistrering
                        }
                    }
                }
            }
        }
    }
})
