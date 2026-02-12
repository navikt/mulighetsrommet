package no.nav.mulighetsrommet.api.utbetaling

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.mulighetsrommet.api.EntraGroupNavAnsattRolleMapping
import no.nav.mulighetsrommet.api.aarsakerforklaring.AarsakerOgForklaringRequest
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontonummerResponse
import no.nav.mulighetsrommet.api.createAuthConfig
import no.nav.mulighetsrommet.api.createTestApplicationConfig
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.TilsagnFixtures
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures
import no.nav.mulighetsrommet.api.getAnsattClaims
import no.nav.mulighetsrommet.api.navansatt.ktor.NavAnsattManglerTilgang
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.utbetaling.api.OpprettUtbetalingRequest
import no.nav.mulighetsrommet.api.utbetaling.api.ValutaBelopRequest
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingReturnertAarsak
import no.nav.mulighetsrommet.api.withTestApplication
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.ktor.MockEngineBuilder
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson
import no.nav.mulighetsrommet.model.Valuta
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.time.LocalDate
import java.util.UUID

class UtbetalingRoutesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val ansatt = NavAnsattFixture.DonaldDuck
    val domain = MulighetsrommetTestDomain(
        ansatte = listOf(ansatt),
        avtaler = listOf(AvtaleFixtures.AFT),
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

    fun mockKontoregisterOrganisasjon(builder: MockEngineBuilder) {
        val path = Regex(""".*/kontoregister/api/v1/hent-kontonummer-for-organisasjon/.*""")
        builder.get(path) {
            respondJson(
                KontonummerResponse(
                    kontonr = "12345678901",
                    mottaker = "asdf",
                ),
            )
        }
    }

    val generellRolle = EntraGroupNavAnsattRolleMapping(UUID.randomUUID(), Rolle.TILTAKADMINISTRASJON_GENERELL)
    val saksbehandlerOkonomiRolle = EntraGroupNavAnsattRolleMapping(UUID.randomUUID(), Rolle.SAKSBEHANDLER_OKONOMI)
    val attestantUtbetalingRolle = EntraGroupNavAnsattRolleMapping(UUID.randomUUID(), Rolle.ATTESTANT_UTBETALING)

    fun appConfig() = createTestApplicationConfig().copy(
        auth = createAuthConfig(
            oauth,
            roles = setOf(generellRolle, saksbehandlerOkonomiRolle, attestantUtbetalingRolle),
        ),
        engine = createMockEngine {
            mockKontoregisterOrganisasjon(this)
        },
    )

    context("opprett utbetaling") {
        test("Skal returnere 400 Bad Request når det er valideringsfeil") {
            withTestApplication(appConfig()) {
                val id = UUID.randomUUID()
                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle, saksbehandlerOkonomiRolle))

                val response = client.post("/api/tiltaksadministrasjon/utbetaling/$id/opprett-utbetaling") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(
                        OpprettUtbetalingRequest(
                            gjennomforingId = AFT1.id,
                            periodeStart = LocalDate.now(),
                            periodeSlutt = LocalDate.now().plusDays(1),
                            beskrivelse = "Kort besk..",
                            kidNummer = null,
                            pris = ValutaBelopRequest(0, Valuta.NOK),
                        ),
                    )
                }
                response.status shouldBe HttpStatusCode.BadRequest
                response.body<ValidationError>().errors shouldBe listOf(
                    FieldError("/pris/belop", "Beløp må være positivt"),
                )
            }
        }

        test("403 Forbidden uten saksbehandler-tilgang") {
            withTestApplication(appConfig()) {
                val id = UUID.randomUUID()
                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle, attestantUtbetalingRolle))

                val response = client.post("/api/tiltaksadministrasjon/utbetaling/$id/opprett-utbetaling") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(
                        OpprettUtbetalingRequest(
                            gjennomforingId = AFT1.id,
                            periodeStart = LocalDate.now(),
                            periodeSlutt = LocalDate.now().plusDays(1),
                            beskrivelse = "Bla bla bla bla bla",
                            kidNummer = null,
                            pris = ValutaBelopRequest(150, Valuta.NOK),
                        ),
                    )
                }
                response.status shouldBe HttpStatusCode.Forbidden
                response.body<NavAnsattManglerTilgang>().missingRoles shouldBe setOf(Rolle.SAKSBEHANDLER_OKONOMI)
            }
        }

        test("Skal returnere 201 med saksbehandler-tilgang") {
            withTestApplication(appConfig()) {
                val id = UUID.randomUUID()
                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle, saksbehandlerOkonomiRolle))

                val response = client.post("/api/tiltaksadministrasjon/utbetaling/$id/opprett-utbetaling") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(
                        OpprettUtbetalingRequest(
                            gjennomforingId = AFT1.id,
                            periodeStart = LocalDate.now(),
                            periodeSlutt = LocalDate.now().plusDays(1),
                            beskrivelse = "Bla bla bla bla bla",
                            kidNummer = null,
                            pris = ValutaBelopRequest(150, Valuta.NOK),
                        ),
                    )
                }
                response.status shouldBe HttpStatusCode.Created
            }
        }
    }

    context("attester utbetaling") {
        test("403 Forbidden uten attestant-tilgang") {
            withTestApplication(appConfig()) {
                val id = UtbetalingFixtures.delutbetaling1.id
                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle, saksbehandlerOkonomiRolle))

                val response = client.post("/api/tiltaksadministrasjon/delutbetalinger/$id/attester") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                }

                response.status shouldBe HttpStatusCode.Forbidden
                response.body<NavAnsattManglerTilgang>().missingRoles shouldBe setOf(Rolle.ATTESTANT_UTBETALING)
            }
        }
    }

    context("returner utbetaling") {
        test("403 Forbidden uten attestant-tilgang") {
            withTestApplication(appConfig()) {
                val id = UtbetalingFixtures.delutbetaling1.id
                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle, saksbehandlerOkonomiRolle))

                val response = client.post("/api/tiltaksadministrasjon/delutbetalinger/$id/returner") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                    setBody(AarsakerOgForklaringRequest(listOf(DelutbetalingReturnertAarsak.FEIL_BELOP), null))
                }

                response.status shouldBe HttpStatusCode.Forbidden
                response.body<NavAnsattManglerTilgang>().missingRoles shouldBe setOf(Rolle.ATTESTANT_UTBETALING)
            }
        }
    }
})
