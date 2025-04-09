package no.nav.mulighetsrommet.api.utbetaling

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import no.nav.mulighetsrommet.api.*
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TilsagnFixtures
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.utbetaling.api.BesluttDelutbetalingRequest
import no.nav.mulighetsrommet.api.utbetaling.api.OpprettManuellUtbetalingRequest
import no.nav.mulighetsrommet.api.utbetaling.api.OpprettManuellUtbetalingRequest.Periode
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.time.LocalDate
import java.util.*

class UtbetalingRoutesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))
    val domain = MulighetsrommetTestDomain(
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

    val generellRolle = AdGruppeNavAnsattRolleMapping(UUID.randomUUID(), Rolle.TILTAKADMINISTRASJON_GENERELL)
    val saksbehandlerOkonomiRolle = AdGruppeNavAnsattRolleMapping(
        UUID.randomUUID(),
        Rolle.SAKSBEHANDLER_OKONOMI,
    )
    val attestantUtbetalingRolle = AdGruppeNavAnsattRolleMapping(UUID.randomUUID(), Rolle.ATTESTANT_UTBETALING)

    fun appConfig(
        engine: HttpClientEngine = CIO.create(),
    ) = createTestApplicationConfig().copy(
        database = databaseConfig,
        auth = createAuthConfig(
            oauth,
            roles = setOf(generellRolle, saksbehandlerOkonomiRolle, attestantUtbetalingRolle),
        ),
        engine = engine,
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
                val response = client.post("/api/v1/intern/utbetaling/$id/opprett-utbetaling") {
                    val claims = mapOf(
                        "NAVident" to "ABC123",
                        "groups" to listOf(
                            generellRolle.adGruppeId,
                            saksbehandlerOkonomiRolle.adGruppeId,
                        ),
                    )
                    bearerAuth(
                        oauth.issueToken(claims = claims).serialize(),
                    )
                    contentType(ContentType.Application.Json)
                    setBody(
                        OpprettManuellUtbetalingRequest(
                            gjennomforingId = AFT1.id,
                            periode = Periode(
                                start = LocalDate.now().plusDays(5),
                                slutt = LocalDate.now().plusDays(1),
                            ),
                            beskrivelse = "Kort besk..",
                            kontonummer = Kontonummer(value = "12345678910"),
                            kidNummer = null,
                            belop = 0,
                        ),
                    )
                }
                println(response.bodyAsText())
                response.status shouldBe HttpStatusCode.BadRequest
            }
        }

        test("Skal returnere 401 uten saksbehandler-tilgang") {
            withTestApplication(appConfig()) {
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val id = UUID.randomUUID()
                val response = client.post("/api/v1/intern/utbetaling/$id/opprett-utbetaling") {
                    val claims = mapOf(
                        "NAVident" to "ABC123",
                        "groups" to listOf(generellRolle.adGruppeId, attestantUtbetalingRolle.adGruppeId),
                    )
                    bearerAuth(
                        oauth.issueToken(claims = claims).serialize(),
                    )
                    contentType(ContentType.Application.Json)
                    setBody(
                        OpprettManuellUtbetalingRequest(
                            gjennomforingId = AFT1.id,
                            periode = Periode(
                                start = LocalDate.now(),
                                slutt = LocalDate.now().plusDays(1),
                            ),
                            beskrivelse = "Bla bla bla bla bla",
                            kontonummer = Kontonummer(value = "12345678910"),
                            kidNummer = null,
                            belop = 150,
                        ),
                    )
                }
                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("Skal returnere 200 ok med saksbehandler-tilgang") {
            withTestApplication(appConfig()) {
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val id = UUID.randomUUID()
                val response = client.post("/api/v1/intern/utbetaling/$id/opprett-utbetaling") {
                    val claims = mapOf(
                        "NAVident" to "ABC123",
                        "groups" to listOf(
                            generellRolle.adGruppeId,
                            saksbehandlerOkonomiRolle.adGruppeId,
                        ),
                    )
                    bearerAuth(
                        oauth.issueToken(claims = claims).serialize(),
                    )
                    contentType(ContentType.Application.Json)
                    setBody(
                        OpprettManuellUtbetalingRequest(
                            gjennomforingId = AFT1.id,
                            periode = Periode(
                                start = LocalDate.now(),
                                slutt = LocalDate.now().plusDays(1),
                            ),
                            beskrivelse = "Bla bla bla bla bla",
                            kontonummer = Kontonummer(value = "12345678910"),
                            kidNummer = null,
                            belop = 150,
                        ),
                    )
                }
                response.status shouldBe HttpStatusCode.OK
            }
        }
    }

    context("beslutt utbetaling") {
        test("Skal returnere 401 uten attestant-tilgang") {
            withTestApplication(appConfig()) {
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val id = UtbetalingFixtures.utbetaling1.id
                val response = client.post("/api/v1/intern/delutbetalinger/$id/beslutt") {
                    val claims = mapOf(
                        "NAVident" to "ABC123",
                        "groups" to listOf(generellRolle.adGruppeId, saksbehandlerOkonomiRolle.adGruppeId),
                    )
                    bearerAuth(oauth.issueToken(claims = claims).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(
                        BesluttDelutbetalingRequest.GodkjentDelutbetalingRequest,
                    )
                }
                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("Skal returnere 200 OK med attestant-tilgang") {
            withTestApplication(appConfig()) {
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val id = UtbetalingFixtures.utbetaling1.id
                val response = client.post("/api/v1/intern/delutbetalinger/$id/beslutt") {
                    val claims = mapOf(
                        "NAVident" to "ABC123",
                        "groups" to listOf(generellRolle.adGruppeId, attestantUtbetalingRolle),
                    )
                    bearerAuth(oauth.issueToken(claims = claims).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(
                        BesluttDelutbetalingRequest.GodkjentDelutbetalingRequest,
                    )
                }
                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }
    }
})
