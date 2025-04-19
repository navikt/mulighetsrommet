package no.nav.mulighetsrommet.api.utbetaling

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import no.nav.mulighetsrommet.api.*
import no.nav.mulighetsrommet.api.clients.msgraph.AdGruppe
import no.nav.mulighetsrommet.api.clients.msgraph.mockMsGraphGetMemberGroups
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TilsagnFixtures
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.utbetaling.api.BesluttDelutbetalingRequest
import no.nav.mulighetsrommet.api.utbetaling.api.OpprettManuellUtbetalingRequest
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.ktor.createMockEngine
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
    val saksbehandlerOkonomiRolle = AdGruppeNavAnsattRolleMapping(UUID.randomUUID(), Rolle.SAKSBEHANDLER_OKONOMI)
    val attestantUtbetalingRolle = AdGruppeNavAnsattRolleMapping(UUID.randomUUID(), Rolle.ATTESTANT_UTBETALING)

    val navAnsattOid = UUID.randomUUID()

    fun appConfig(
        roller: Set<AdGruppeNavAnsattRolleMapping> = setOf(
            generellRolle,
            saksbehandlerOkonomiRolle,
            attestantUtbetalingRolle,
        ),
    ) = createTestApplicationConfig().copy(
        database = databaseConfig,
        auth = createAuthConfig(
            oauth,
            roles = roller,
        ),
        engine = createMockEngine {
            mockMsGraphGetMemberGroups(navAnsattOid) {
                roller.map { AdGruppe(id = it.adGruppeId, navn = it.rolle.name) }
            }
        },
    )

    val navAnsattClaims = mapOf(
        "NAVident" to "ABC123",
        "oid" to navAnsattOid.toString(),
    )

    context("opprett utbetaling") {
        test("Skal returnere 400 Bad Request når det er valideringsfeil") {
            withTestApplication(appConfig(setOf(generellRolle, saksbehandlerOkonomiRolle))) {
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val id = UUID.randomUUID()
                val response = client.post("/api/v1/intern/utbetaling/$id/opprett-utbetaling") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(
                        OpprettManuellUtbetalingRequest(
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
                    FieldError.ofPointer("/arrangorinfo/belop", "Beløp må være positivt"),
                )
            }
        }

        test("403 Forbidden uten saksbehandler-tilgang") {
            withTestApplication(appConfig(setOf(generellRolle, attestantUtbetalingRolle))) {
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val id = UUID.randomUUID()
                val response = client.post("/api/v1/intern/utbetaling/$id/opprett-utbetaling") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(
                        OpprettManuellUtbetalingRequest(
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
                response.bodyAsText() shouldBe "Mangler følgende rolle: SAKSBEHANDLER_OKONOMI"
            }
        }

        test("Skal returnere 200 ok med saksbehandler-tilgang") {
            withTestApplication(appConfig(setOf(generellRolle, saksbehandlerOkonomiRolle))) {
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val id = UUID.randomUUID()
                val response = client.post("/api/v1/intern/utbetaling/$id/opprett-utbetaling") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(
                        OpprettManuellUtbetalingRequest(
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
                response.status shouldBe HttpStatusCode.OK
            }
        }
    }

    context("beslutt utbetaling") {
        test("403 Forbidden uten attestant-tilgang") {
            withTestApplication(appConfig(setOf(generellRolle, saksbehandlerOkonomiRolle))) {
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val id = UtbetalingFixtures.utbetaling1.id
                val response = client.post("/api/v1/intern/delutbetalinger/$id/beslutt") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(BesluttDelutbetalingRequest.GodkjentDelutbetalingRequest)
                }
                response.status shouldBe HttpStatusCode.Forbidden
                response.bodyAsText() shouldBe "Mangler følgende rolle: ATTESTANT_UTBETALING"
            }
        }

        // TODO: fiks test - tittel matcher ikke forventet status
        xtest("Skal returnere 200 OK med attestant-tilgang") {
            withTestApplication(appConfig(setOf(generellRolle, attestantUtbetalingRolle))) {
                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val id = UtbetalingFixtures.utbetaling1.id
                val response = client.post("/api/v1/intern/delutbetalinger/$id/beslutt") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
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
