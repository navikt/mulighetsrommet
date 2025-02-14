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
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattRolle
import no.nav.mulighetsrommet.api.utbetaling.OpprettManuellUtbetalingRequest.Periode
import no.nav.mulighetsrommet.api.utbetaling.db.DelutbetalingDbo
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
        delutbetalinger = listOf(
            DelutbetalingDbo(
                tilsagnId = TilsagnFixtures.Tilsagn1.id,
                utbetalingId = UtbetalingFixtures.utbetaling1.id,
                belop = 100,
                periode = no.nav.mulighetsrommet.model.Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 1)),
                lopenummer = 1,
                fakturanummer = "2025/1",
                opprettetAv = NavAnsattFixture.ansatt1.navIdent,
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
    val avtaleSkrivRolle = AdGruppeNavAnsattRolleMapping(UUID.randomUUID(), NavAnsattRolle.AVTALER_SKRIV)
    val gjennomforingerSkrivRolle =
        AdGruppeNavAnsattRolleMapping(UUID.randomUUID(), NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV)
    val beslutterRolle = AdGruppeNavAnsattRolleMapping(UUID.randomUUID(), NavAnsattRolle.OKONOMI_BESLUTTER)

    fun appConfig(
        engine: HttpClientEngine = CIO.create(),
    ) = createTestApplicationConfig().copy(
        database = databaseConfig,
        auth = createAuthConfig(oauth, roles = listOf(generellRolle, avtaleSkrivRolle, gjennomforingerSkrivRolle, beslutterRolle)),
        engine = engine,
    )

    test("Skal returnere 200 ok for korrekt request for manuell utbetaling (frimodell)") {
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
                        avtaleSkrivRolle.adGruppeId,
                        generellRolle.adGruppeId,
                        gjennomforingerSkrivRolle.adGruppeId,
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

    test("Skal returnere 400 Bad Request når det er valideringsfeil ved manuell utbetaling (frimodell)") {
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
                        avtaleSkrivRolle.adGruppeId,
                        generellRolle.adGruppeId,
                        gjennomforingerSkrivRolle.adGruppeId,
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

    test("Skal returnere 401 uten skrive tilgang") {
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
                    "groups" to listOf(generellRolle.adGruppeId),
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

    test("Skal returnere 401 uten beslutter tilgang") {
        withTestApplication(appConfig()) {
            val client = createClient {
                install(ContentNegotiation) {
                    json()
                }
            }

            val id = UtbetalingFixtures.utbetaling1.id
            val response = client.post("/api/v1/intern/utbetaling/$id/delutbetaling/beslutt") {
                val claims = mapOf(
                    "NAVident" to "ABC123",
                    "groups" to listOf(generellRolle.adGruppeId),
                )
                bearerAuth(oauth.issueToken(claims = claims).serialize())
                contentType(ContentType.Application.Json)
                setBody(
                    BesluttDelutbetalingRequest.GodkjentDelutbetalingRequest(
                        tilsagnId = TilsagnFixtures.Tilsagn1.id,
                    ),
                )
            }
            response.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    test("Skal returnere 200 OK med okonomi beslutter tilgang") {
        withTestApplication(appConfig()) {
            val client = createClient {
                install(ContentNegotiation) {
                    json()
                }
            }

            val id = UtbetalingFixtures.utbetaling1.id
            val response = client.post("/api/v1/intern/utbetaling/$id/delutbetaling/beslutt") {
                val claims = mapOf(
                    "NAVident" to "ABC123",
                    "groups" to listOf(generellRolle.adGruppeId, beslutterRolle),
                )
                bearerAuth(oauth.issueToken(claims = claims).serialize())
                contentType(ContentType.Application.Json)
                setBody(
                    BesluttDelutbetalingRequest.GodkjentDelutbetalingRequest(
                        tilsagnId = TilsagnFixtures.Tilsagn1.id,
                    ),
                )
            }
            response.status shouldBe HttpStatusCode.Unauthorized
        }
    }
})
