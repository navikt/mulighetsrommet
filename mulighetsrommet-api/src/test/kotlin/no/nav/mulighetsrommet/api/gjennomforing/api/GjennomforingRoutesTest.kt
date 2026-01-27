package no.nav.mulighetsrommet.api.gjennomforing.api

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import io.kotest.matchers.types.shouldBeTypeOf
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.mulighetsrommet.api.EntraGroupNavAnsattRolleMapping
import no.nav.mulighetsrommet.api.aarsakerforklaring.AarsakerOgForklaringRequest
import no.nav.mulighetsrommet.api.createAuthConfig
import no.nav.mulighetsrommet.api.createTestApplicationConfig
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures
import no.nav.mulighetsrommet.api.getAnsattClaims
import no.nav.mulighetsrommet.api.gjennomforing.model.AvbrytGjennomforingAarsak
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingStatus
import no.nav.mulighetsrommet.api.navansatt.ktor.NavAnsattManglerTilgang
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.withTestApplication
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.time.LocalDate
import java.util.UUID

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
        )

        beforeEach {
            domain.initialize(database.db)
        }

        afterEach {
            database.truncateAll()
        }

        test("401 Unauthorized for uautentisert kall") {
            withTestApplication(appConfig()) {
                val response = client.get("/api/tiltaksadministrasjon/gjennomforinger")
                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("403 Forbidden når bruker ikke har generell tilgang") {
            withTestApplication(appConfig()) {
                val navAnsattClaims = getAnsattClaims(ansatt, setOf(gjennomforingSkrivRolle))

                val response = client.get("/api/tiltaksadministrasjon/gjennomforinger") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                }

                response.status shouldBe HttpStatusCode.Forbidden
                response.body<NavAnsattManglerTilgang>().missingRoles shouldBe setOf(Rolle.TILTAKADMINISTRASJON_GENERELL)
            }
        }

        test("200 OK med generell tilgang") {
            withTestApplication(appConfig()) {
                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle))

                val response = client.get("/api/tiltaksadministrasjon/gjennomforinger") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                }

                response.status shouldBe HttpStatusCode.OK
            }
        }
    }

    context("opprett gjennomføring") {
        val avtale = AvtaleFixtures.oppfolging.copy(
            veilederinformasjonDbo = AvtaleFixtures.veilederinformasjonDbo(
                navEnheter = setOf(
                    NavEnhetFixtures.Oslo.enhetsnummer,
                    NavEnhetFixtures.Sagene.enhetsnummer,
                ),
            ),
        )

        val domain = MulighetsrommetTestDomain(
            navEnheter = listOf(NavEnhetFixtures.Innlandet, NavEnhetFixtures.Oslo, NavEnhetFixtures.Sagene),
            ansatte = listOf(ansatt),
            arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
            avtaler = listOf(avtale),
        )

        beforeEach {
            domain.initialize(database.db)
        }

        afterEach {
            database.truncateAll()
        }

        test("403 Forbidden når bruker mangler generell tilgang") {
            withTestApplication(appConfig()) {
                val navAnsattClaims = getAnsattClaims(ansatt, setOf(gjennomforingSkrivRolle))

                val response = client.put("/api/tiltaksadministrasjon/gjennomforinger") {
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
                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle))

                val response = client.put("/api/tiltaksadministrasjon/gjennomforinger") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                }

                response.status shouldBe HttpStatusCode.Forbidden
                response.body<NavAnsattManglerTilgang>().missingRoles shouldBe setOf(Rolle.TILTAKSGJENNOMFORINGER_SKRIV)
            }
        }

        test("400 Bad Request når gjennomføringen er ugyldig") {
            withTestApplication(appConfig()) {
                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle, gjennomforingSkrivRolle))

                val response = client.put("/api/tiltaksadministrasjon/gjennomforinger") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(
                        GjennomforingFixtures.createGjennomforingRequest(
                            avtale,
                            administratorer = emptyList(),
                            navRegioner = listOf(NavEnhetFixtures.Oslo.enhetsnummer),
                            navKontorer = listOf(NavEnhetFixtures.Sagene.enhetsnummer),
                        ),
                    )
                }

                response.status shouldBe HttpStatusCode.BadRequest
                response.body<ValidationError>().errors shouldBe listOf(
                    FieldError.of(
                        "Du må velge minst én administrator",
                        GjennomforingRequest::administratorer,
                    ),
                )
            }
        }

        test("200 OK når bruker har skrivetilgang til gjennomføringer") {
            withTestApplication(appConfig()) {
                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle, gjennomforingSkrivRolle))

                val response = client.put("/api/tiltaksadministrasjon/gjennomforinger") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(
                        GjennomforingFixtures.createGjennomforingRequest(
                            avtale,
                            navRegioner = listOf(NavEnhetFixtures.Oslo.enhetsnummer),
                            navKontorer = listOf(NavEnhetFixtures.Sagene.enhetsnummer),
                        ),
                    )
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
                    sluttDato = LocalDate.now().plusDays(20),
                    status = GjennomforingStatusType.GJENNOMFORES,
                ),
                GjennomforingFixtures.Oppfolging1.copy(
                    id = avbruttGjennomforingId,
                ),
            ),
        ) {
            queries.gjennomforing.setStatus(
                id = avbruttGjennomforingId,
                status = GjennomforingStatusType.AVBRUTT,
                sluttDato = LocalDate.now(),
                aarsaker = listOf(AvbrytGjennomforingAarsak.FEILREGISTRERING),
                forklaring = null,
            )
        }

        beforeEach {
            domain.initialize(database.db)
        }

        afterEach {
            database.truncateAll()
        }

        test("bad request når årsak mangler") {
            withTestApplication(appConfig()) {
                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle, gjennomforingSkrivRolle))

                val response = client
                    .put("/api/tiltaksadministrasjon/gjennomforinger/$aktivGjennomforingId/avbryt") {
                        bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                        contentType(ContentType.Application.Json)
                        setBody("{}")
                    }

                response.status shouldBe HttpStatusCode.BadRequest
            }
        }

        test("bad request når beskrivelse mangler for Annen årsak") {
            withTestApplication(appConfig()) {
                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle, gjennomforingSkrivRolle))

                val response = client
                    .put("/api/tiltaksadministrasjon/gjennomforinger/$aktivGjennomforingId/avbryt") {
                        bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                        contentType(ContentType.Application.Json)
                        setBody(
                            AvbrytGjennomforingRequest(
                                aarsakerOgForklaringRequest = AarsakerOgForklaringRequest(
                                    aarsaker = listOf(AvbrytGjennomforingAarsak.ANNET),
                                    forklaring = null,
                                ),
                                dato = LocalDate.now(),
                            ),
                        )
                    }

                response.status shouldBe HttpStatusCode.BadRequest
            }
        }

        test("bad request når gjennomføring allerede er avbrutt") {
            withTestApplication(appConfig()) {
                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle, gjennomforingSkrivRolle))

                val response = client
                    .put("/api/tiltaksadministrasjon/gjennomforinger/$avbruttGjennomforingId/avbryt") {
                        bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                        contentType(ContentType.Application.Json)
                        setBody(
                            AvbrytGjennomforingRequest(
                                aarsakerOgForklaringRequest = AarsakerOgForklaringRequest(
                                    aarsaker = listOf(AvbrytGjennomforingAarsak.FEILREGISTRERING),
                                    forklaring = null,
                                ),
                                dato = LocalDate.now(),
                            ),
                        )
                    }

                response.status shouldBe HttpStatusCode.BadRequest
                response.body<ValidationError>().errors shouldBe listOf(
                    FieldError("/", "Gjennomføringen er allerede avbrutt"),
                )
            }
        }

        test("avbryter gjennomføring") {
            withTestApplication(appConfig()) {
                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle, gjennomforingSkrivRolle))

                val response = client.put("/api/tiltaksadministrasjon/gjennomforinger/$aktivGjennomforingId/avbryt") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(
                        AvbrytGjennomforingRequest(
                            aarsakerOgForklaringRequest = AarsakerOgForklaringRequest(
                                aarsaker = listOf(AvbrytGjennomforingAarsak.FEILREGISTRERING),
                                forklaring = null,
                            ),
                            dato = LocalDate.now(),
                        ),
                    )
                }

                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText().shouldBeEmpty()

                database.run {
                    queries.gjennomforing.getGruppetiltakOrError(aktivGjennomforingId).should {
                        it.status.shouldBeTypeOf<GjennomforingStatus.Avbrutt>().should {
                            it.type shouldBe GjennomforingStatusType.AVBRUTT
                            it.aarsaker shouldContain AvbrytGjennomforingAarsak.FEILREGISTRERING
                        }
                    }
                }
            }
        }
    }
})
