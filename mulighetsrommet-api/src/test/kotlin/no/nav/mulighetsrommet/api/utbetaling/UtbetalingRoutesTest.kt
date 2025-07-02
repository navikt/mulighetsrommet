package no.nav.mulighetsrommet.api.utbetaling

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import no.nav.mulighetsrommet.api.*
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.TilsagnFixtures
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures
import no.nav.mulighetsrommet.api.navansatt.ktor.NavAnsattManglerTilgang
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.utbetaling.api.BesluttDelutbetalingRequest
import no.nav.mulighetsrommet.api.utbetaling.api.OpprettUtbetalingRequest
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.time.LocalDate
import java.util.*

class UtbetalingRoutesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val ansatt = NavAnsattFixture.DonaldDuck
    val domain = MulighetsrommetTestDomain(
        ansatte = listOf(ansatt),
        gjennomforinger = listOf(AFT1),
        tilsagn = listOf(TilsagnFixtures.Tilsagn1),
        utbetalinger = listOf(UtbetalingFixtures.utbetaling1),
        delutbetalinger = listOf(UtbetalingFixtures.delutbetaling1),
    )

    val oauth = MockOAuth2Server()

    beforeSpec {
        oauth.start()
        domain.initialize(database.db)
    }

    afterSpec {
        oauth.shutdown()
    }

    val generellRolle = EntraGroupNavAnsattRolleMapping(UUID.randomUUID(), Rolle.TILTAKADMINISTRASJON_GENERELL)
    val saksbehandlerOkonomiRolle = EntraGroupNavAnsattRolleMapping(UUID.randomUUID(), Rolle.SAKSBEHANDLER_OKONOMI)
    val attestantUtbetalingRolle = EntraGroupNavAnsattRolleMapping(UUID.randomUUID(), Rolle.ATTESTANT_UTBETALING)

    fun appConfig() = createTestApplicationConfig().copy(
        auth = createAuthConfig(
            oauth,
            roles = setOf(generellRolle, saksbehandlerOkonomiRolle, attestantUtbetalingRolle),
        ),
    )

    context("opprett utbetaling") {
        test("Skal returnere 400 Bad Request når det er valideringsfeil") {
            withTestApplication(appConfig()) {
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val id = UUID.randomUUID()
                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle, saksbehandlerOkonomiRolle))

                val response = client.post("/api/v1/intern/utbetaling/$id/opprett-utbetaling") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(
                        OpprettUtbetalingRequest(
                            gjennomforingId = AFT1.id,
                            periodeStart = LocalDate.now(),
                            periodeSlutt = LocalDate.now().plusDays(1),
                            beskrivelse = "Kort besk..",
                            kontonummer = Kontonummer(value = "12345678910"),
                            kidNummer = null,
                            belop = 0,
                        ),
                    )
                }
                response.status shouldBe HttpStatusCode.BadRequest
                response.body<ValidationError>().errors shouldBe listOf(
                    FieldError.ofPointer("/belop", "Beløp må være positivt"),
                )
            }
        }

        test("403 Forbidden uten saksbehandler-tilgang") {
            withTestApplication(appConfig()) {
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val id = UUID.randomUUID()
                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle, attestantUtbetalingRolle))

                val response = client.post("/api/v1/intern/utbetaling/$id/opprett-utbetaling") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(
                        OpprettUtbetalingRequest(
                            gjennomforingId = AFT1.id,
                            periodeStart = LocalDate.now(),
                            periodeSlutt = LocalDate.now().plusDays(1),
                            beskrivelse = "Bla bla bla bla bla",
                            kontonummer = Kontonummer(value = "12345678910"),
                            kidNummer = null,
                            belop = 150,
                        ),
                    )
                }
                response.status shouldBe HttpStatusCode.Forbidden
                response.body<NavAnsattManglerTilgang>().missingRoles shouldBe setOf(Rolle.SAKSBEHANDLER_OKONOMI)
            }
        }

        test("Skal returnere 201 med saksbehandler-tilgang") {
            withTestApplication(appConfig()) {
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val id = UUID.randomUUID()
                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle, saksbehandlerOkonomiRolle))

                val response = client.post("/api/v1/intern/utbetaling/$id/opprett-utbetaling") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(
                        OpprettUtbetalingRequest(
                            gjennomforingId = AFT1.id,
                            periodeStart = LocalDate.now(),
                            periodeSlutt = LocalDate.now().plusDays(1),
                            beskrivelse = "Bla bla bla bla bla",
                            kontonummer = Kontonummer(value = "12345678910"),
                            kidNummer = null,
                            belop = 150,
                        ),
                    )
                }
                response.status shouldBe HttpStatusCode.Created
            }
        }
    }

    context("beslutt utbetaling") {
        test("403 Forbidden uten attestant-tilgang") {
            withTestApplication(appConfig()) {
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val id = UtbetalingFixtures.utbetaling1.id
                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle, saksbehandlerOkonomiRolle))

                val response = client.post("/api/v1/intern/delutbetalinger/$id/beslutt") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(BesluttDelutbetalingRequest.Godkjent)
                }
                response.status shouldBe HttpStatusCode.Forbidden
                response.body<NavAnsattManglerTilgang>().missingRoles shouldBe setOf(Rolle.ATTESTANT_UTBETALING)
            }
        }

        // TODO: fiks test - tittel matcher ikke forventet status
        xtest("Skal returnere 200 OK med attestant-tilgang") {
            withTestApplication(appConfig()) {
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val id = UtbetalingFixtures.utbetaling1.id
                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle, attestantUtbetalingRolle))

                val response = client.post("/api/v1/intern/delutbetalinger/$id/beslutt") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(
                        BesluttDelutbetalingRequest.Godkjent,
                    )
                }
                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }
    }
})
