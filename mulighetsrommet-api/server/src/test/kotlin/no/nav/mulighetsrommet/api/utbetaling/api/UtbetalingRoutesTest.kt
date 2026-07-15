package no.nav.mulighetsrommet.api.utbetaling.api

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.mulighetsrommet.api.ApplicationConfigTest
import no.nav.mulighetsrommet.api.EntraGroupNavAnsattRolleMapping
import no.nav.mulighetsrommet.api.aarsakerforklaring.AarsakerOgForklaringRequest
import no.nav.mulighetsrommet.api.clients.pdl.PdlGradering
import no.nav.mulighetsrommet.api.createAuthConfig
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.DeltakerFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.TilsagnFixtures
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures
import no.nav.mulighetsrommet.api.getAnsattClaims
import no.nav.mulighetsrommet.api.mockAmtDeltakerPersonalia
import no.nav.mulighetsrommet.api.mockKontoregisterOrganisasjon
import no.nav.mulighetsrommet.api.mockPdlEmptyResult
import no.nav.mulighetsrommet.api.mockTilgangsmaskinenForbidden
import no.nav.mulighetsrommet.api.navansatt.ktor.NavAnsattManglerTilgang
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelseDeltakelsesprosentPerioder
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelsesprosentPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.SatsPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFastSatsPerTiltaksplassPerManed
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningOutputDeltakelse
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingLinjeReturnertAarsak
import no.nav.mulighetsrommet.api.utbetaling.service.Gradering
import no.nav.mulighetsrommet.api.withTestApplication
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.model.NOK
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.time.LocalDate
import java.util.UUID

class UtbetalingRoutesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

    val ansatt = NavAnsattFixture.DonaldDuck
    val domain = MulighetsrommetTestDomain(
        ansatte = listOf(ansatt),
        avtaler = listOf(AvtaleFixtures.AFT),
        gjennomforinger = listOf(AFT1),
        tilsagn = listOf(TilsagnFixtures.Tilsagn1),
        utbetalinger = listOf(UtbetalingFixtures.utbetaling1),
        utbetalingLinjer = listOf(UtbetalingFixtures.utbetalingLinje1),
    )

    val oauth = MockOAuth2Server()

    beforeSpec {
        oauth.start()
        domain.initialize(database.api)
    }

    afterSpec {
        oauth.shutdown()
    }

    val generellRolle = EntraGroupNavAnsattRolleMapping(UUID.randomUUID(), Rolle.TILTAKADMINISTRASJON_GENERELL)
    val saksbehandlerOkonomiRolle = EntraGroupNavAnsattRolleMapping(UUID.randomUUID(), Rolle.SAKSBEHANDLER_OKONOMI)
    val attestantUtbetalingRolle = EntraGroupNavAnsattRolleMapping(UUID.randomUUID(), Rolle.ATTESTANT_UTBETALING)

    fun appConfig() = ApplicationConfigTest.copy(
        auth = createAuthConfig(
            oauth,
            roles = setOf(generellRolle, saksbehandlerOkonomiRolle, attestantUtbetalingRolle),
        ),
        engine = createMockEngine {
            mockKontoregisterOrganisasjon()
        },
    )

    context("opprett utbetaling") {
        test("403 Forbidden uten saksbehandler-tilgang") {
            withTestApplication(appConfig()) {
                val id = UUID.randomUUID()
                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle, attestantUtbetalingRolle))

                val response = client.post("/api/tiltaksadministrasjon/utbetaling/opprett") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(
                        UtbetalingRequest(
                            id = id,
                            gjennomforingId = AFT1.id,
                            periodeStart = LocalDate.now(),
                            periodeSlutt = LocalDate.now().plusDays(1),
                            pris = ValutaBelopRequest(150, Valuta.NOK),
                        ),
                    )
                }

                response.status shouldBe HttpStatusCode.Forbidden
                response.body<NavAnsattManglerTilgang>().missingRoles shouldBe setOf(Rolle.SAKSBEHANDLER_OKONOMI)
            }
        }

        test("validerer påkrevde felter") {
            withTestApplication(appConfig()) {
                val id = UUID.randomUUID()
                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle, saksbehandlerOkonomiRolle))

                val response = client.post("/api/tiltaksadministrasjon/utbetaling/opprett") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(
                        UtbetalingRequest(id = id, gjennomforingId = AFT1.id, journalpostId = "foo"),
                    )
                }

                response.status shouldBe HttpStatusCode.BadRequest
                response.body<ValidationError>().errors shouldContainExactlyInAnyOrder listOf(
                    FieldError("/periodeStart", "Periodestart må være satt"),
                    FieldError("/periodeSlutt", "Periodeslutt må være satt"),
                    FieldError("/pris/belop", "Beløp må være positivt"),
                    FieldError("/journalpostId", "Journalpost-ID er på ugyldig format"),
                )
            }
        }

        test("Skal returnere 201 med saksbehandler-tilgang") {
            withTestApplication(appConfig()) {
                val id = UUID.randomUUID()
                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle, saksbehandlerOkonomiRolle))

                val response = client.post("/api/tiltaksadministrasjon/utbetaling/opprett") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(
                        UtbetalingRequest(
                            id = id,
                            gjennomforingId = AFT1.id,
                            periodeStart = LocalDate.now(),
                            periodeSlutt = LocalDate.now().plusDays(1),
                            pris = ValutaBelopRequest(150, Valuta.NOK),
                            journalpostId = "123",
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
                val id = UtbetalingFixtures.utbetalingLinje1.id
                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle, saksbehandlerOkonomiRolle))

                val response = client.post("/api/tiltaksadministrasjon/utbetalingslinjer/$id/attester") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                }

                response.status shouldBe HttpStatusCode.Forbidden
                response.body<NavAnsattManglerTilgang>().missingRoles shouldBe setOf(Rolle.ATTESTANT_UTBETALING)
            }
        }
    }

    context("returner utbetaling") {
        test("403 forbidden uten saksbehandler- eller beslutter-tilgang") {
            withTestApplication(appConfig()) {
                val id = UtbetalingFixtures.utbetalingLinje1.id
                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle))

                val response = client.post("/api/tiltaksadministrasjon/utbetalingslinjer/$id/returner") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                    setBody(AarsakerOgForklaringRequest(listOf(UtbetalingLinjeReturnertAarsak.FEIL_BELOP), null))
                }

                response.status shouldBe HttpStatusCode.Forbidden
                response.body<NavAnsattManglerTilgang>().missingRoles shouldBe setOf(
                    Rolle.SAKSBEHANDLER_OKONOMI,
                    Rolle.BESLUTTER_TILSAGN,
                )
            }
        }

        test("400 bad request når utbetalingen kan ikke er til attestering") {
            withTestApplication(appConfig()) {
                val id = UtbetalingFixtures.utbetalingLinje1.id
                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle, saksbehandlerOkonomiRolle))

                val response = client.post("/api/tiltaksadministrasjon/utbetalingslinjer/$id/returner") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                    setBody(AarsakerOgForklaringRequest(listOf(UtbetalingLinjeReturnertAarsak.FEIL_BELOP), null))
                }

                response.status shouldBe HttpStatusCode.BadRequest
                response.body<ValidationError>().errors shouldBe listOf(
                    FieldError.of("Utbetalingen kan ikke returneres"),
                )
            }
        }
    }

    context("opprett utbetalingslinjer") {
        test("tom liste gir feil om manglende utbetalingslinjer") {
            withTestApplication(appConfig()) {
                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle, saksbehandlerOkonomiRolle))

                val response = client.put("/api/tiltaksadministrasjon/utbetalingslinjer") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(
                        OpprettUtbetalingLinjerRequest(
                            utbetalingId = UtbetalingFixtures.utbetaling1.id,
                            utbetalingLinjer = emptyList(),
                            begrunnelseMindreBetalt = null,
                        ),
                    )
                }

                response.status shouldBe HttpStatusCode.BadRequest
                response.body<ValidationError>().errors shouldContainExactlyInAnyOrder listOf(
                    FieldError("/utbetalingLinjer", "Utbetalingslinjer mangler"),
                )
            }
        }

        test("liste med kun nullbeløp gir feil om manglende utbetalingslinjer") {
            withTestApplication(appConfig()) {
                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle, saksbehandlerOkonomiRolle))

                val response = client.put("/api/tiltaksadministrasjon/utbetalingslinjer") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(
                        OpprettUtbetalingLinjerRequest(
                            utbetalingId = UtbetalingFixtures.utbetaling1.id,
                            utbetalingLinjer = listOf(
                                UtbetalingLinjeRequest(
                                    id = UUID.randomUUID(),
                                    tilsagnId = TilsagnFixtures.Tilsagn1.id,
                                    pris = ValutaBelopRequest(belop = 0, valuta = Valuta.NOK),
                                    gjorOppTilsagn = false,
                                ),
                            ),
                            begrunnelseMindreBetalt = null,
                        ),
                    )
                }

                response.status shouldBe HttpStatusCode.BadRequest
                response.body<ValidationError>().errors shouldContainExactlyInAnyOrder listOf(
                    FieldError("/utbetalingLinjer", "Utbetalingslinjer mangler"),
                )
            }
        }

        test("negativt beløp gir valideringsfeil") {
            withTestApplication(appConfig()) {
                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle, saksbehandlerOkonomiRolle))

                val response = client.put("/api/tiltaksadministrasjon/utbetalingslinjer") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                    contentType(ContentType.Application.Json)
                    setBody(
                        OpprettUtbetalingLinjerRequest(
                            utbetalingId = UtbetalingFixtures.utbetaling1.id,
                            utbetalingLinjer = listOf(
                                UtbetalingLinjeRequest(
                                    id = UUID.randomUUID(),
                                    tilsagnId = TilsagnFixtures.Tilsagn1.id,
                                    pris = ValutaBelopRequest(belop = -100, valuta = Valuta.NOK),
                                    gjorOppTilsagn = false,
                                ),
                            ),
                            begrunnelseMindreBetalt = null,
                        ),
                    )
                }

                response.status shouldBe HttpStatusCode.BadRequest
                response.body<ValidationError>().errors shouldContainExactlyInAnyOrder listOf(
                    FieldError("/utbetalingLinjer/0/pris/belop", "Beløp må være positivt"),
                )
            }
        }
    }

    context("personvern i beregning") {
        val deltaker = DeltakerFixtures.createDeltakerDbo(AFT1.id)

        val beregningPeriode = Periode.forMonthOf(LocalDate.of(2024, 8, 1))
        val utbetaling = UtbetalingFixtures.utbetaling1.copy(
            id = UUID.randomUUID(),
            periode = beregningPeriode,
            beregning = UtbetalingBeregningFastSatsPerTiltaksplassPerManed(
                input = UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Input(
                    satser = setOf(SatsPeriode(periode = beregningPeriode, sats = 20205.NOK)),
                    stengt = setOf(),
                    deltakelser = setOf(
                        DeltakelseDeltakelsesprosentPerioder(
                            deltakelseId = deltaker.id,
                            perioder = listOf(DeltakelsesprosentPeriode(beregningPeriode, 100.0)),
                        ),
                    ),
                ),
                output = UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output(
                    pris = 20205.NOK,
                    deltakelser = setOf(
                        UtbetalingBeregningOutputDeltakelse(
                            deltaker.id,
                            setOf(
                                UtbetalingBeregningOutputDeltakelse.BeregnetPeriode(beregningPeriode, 1.0, 20205.NOK),
                            ),
                        ),
                    ),
                ),
            ),
        )

        fun appConfigMedTilgangsmaskinAvvist() = appConfig().copy(
            engine = createMockEngine {
                mockKontoregisterOrganisasjon()
                mockPdlEmptyResult()
                mockAmtDeltakerPersonalia(gradering = PdlGradering.UGRADERT)
                mockTilgangsmaskinenForbidden(avvistGrunn = "AVVIST_SKJERMING")
            },
        )

        test("personalia er maskert i GET /beregning når NavAnsatt mangler tilgang") {
            MulighetsrommetTestDomain(
                utbetalinger = listOf(utbetaling),
                deltakere = listOf(deltaker),
            ).initialize(database.api)

            withTestApplication(appConfigMedTilgangsmaskinAvvist()) {
                val navAnsattClaims = getAnsattClaims(ansatt, setOf(generellRolle, saksbehandlerOkonomiRolle))

                val response = client.get("/api/tiltaksadministrasjon/utbetaling/${utbetaling.id}/beregning") {
                    bearerAuth(oauth.issueToken(claims = navAnsattClaims).serialize())
                }

                response.status shouldBe HttpStatusCode.OK

                val body = response.body<UtbetalingBeregningDto>()
                body.deltakere.shouldHaveSize(1)

                val deltaker = body.deltakere.first()
                deltaker.navn shouldBe "Skjermet"
                deltaker.norskIdent.shouldBeNull()
                deltaker.geografiskEnhet.shouldBeNull()
                deltaker.gradering shouldBe Gradering.SKJERMING
            }
        }
    }
})
