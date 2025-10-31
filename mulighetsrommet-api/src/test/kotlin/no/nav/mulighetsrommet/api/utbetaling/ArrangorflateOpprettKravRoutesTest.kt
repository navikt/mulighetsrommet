package no.nav.mulighetsrommet.api.utbetaling

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import java.time.LocalDate
import no.nav.mulighetsrommet.api.OkonomiConfig
import no.nav.mulighetsrommet.api.arrangorflate.api.GjennomforingerTableResponse
import no.nav.mulighetsrommet.api.arrangorflate.api.OpprettKravInnsendingsInformasjon
import no.nav.mulighetsrommet.api.arrangorflate.api.OpprettKravInnsendingsInformasjon.DatoVelger
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.utbetaling.ArrangorflateTestUtils.hovedenhet
import no.nav.mulighetsrommet.api.withTestApplication
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.fail

class ArrangorflateOpprettKravRoutesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val identMedTilgang = ArrangorflateTestUtils.identMedTilgang
    val underenhet = ArrangorflateTestUtils.underenhet
    val orgnr = underenhet.organisasjonsnummer.value

    val deltaker = ArrangorflateTestUtils.createTestDeltaker()
    val tilsagn = ArrangorflateTestUtils.createTestTilsagn()
    val aftGjennomforing = GjennomforingFixtures.AFT1
    val oppfolgingGjennomforing = GjennomforingFixtures.Oppfolging1
    val arrGjennomforing = GjennomforingFixtures.ArbeidsrettetRehabilitering

    val domain = MulighetsrommetTestDomain(
        navEnheter = listOf(NavEnhetFixtures.Innlandet, NavEnhetFixtures.Gjovik),
        ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
        tiltakstyper = listOf(TiltakstypeFixtures.AFT, TiltakstypeFixtures.Oppfolging, TiltakstypeFixtures.ArbeidsrettetRehabilitering),
        deltakere = listOf(deltaker),
        arrangorer = listOf(hovedenhet, ArrangorflateTestUtils.underenhet),
        tilsagn = listOf(tilsagn),
        gjennomforinger = listOf(
            aftGjennomforing,
            oppfolgingGjennomforing,
            arrGjennomforing,
        ),
        avtaler = listOf(
            AvtaleFixtures.AFT, // Forhåndsgodkjent
            AvtaleFixtures.oppfolging, // Avtalt pris per time oppfølging
            AvtaleFixtures.ARR, // Annen avtalt pris
        ),
    ) {
        setTilsagnStatus(tilsagn, TilsagnStatus.GODKJENT)
    }

    val oauth = MockOAuth2Server()

    beforeSpec {
        oauth.start()
    }

    beforeEach {
        domain.initialize(database.db)
    }

    afterEach {
        database.truncateAll()
    }

    afterSpec {
        oauth.shutdown()
    }

    test("401 Unauthorized mangler pid i claims") {
        withTestApplication(ArrangorflateTestUtils.appConfig(oauth)) {
            val response = client.get("/api/arrangorflate/arrangor/$orgnr/gjennomforing/opprett-krav") {
                bearerAuth(oauth.issueToken().serialize())
            }
            response.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    test("403 Forbidden feil pid i claims") {
        withTestApplication(ArrangorflateTestUtils.appConfig(oauth)) {
            val response = client.get("/api/arrangorflate/arrangor/$orgnr/gjennomforing/opprett-krav") {
                bearerAuth(oauth.issueToken(claims = mapOf("pid" to "01010199989")).serialize())
            }
            response.status shouldBe HttpStatusCode.Forbidden
        }
    }

    test("200 Ok med rett pid") {
        withTestApplication(ArrangorflateTestUtils.appConfig(oauth)) {
            val response = client.get("/api/arrangorflate/arrangor/$orgnr/gjennomforing/opprett-krav") {
                bearerAuth(oauth.issueToken(claims = mapOf("pid" to identMedTilgang.value)).serialize())
            }

            response.status shouldBe HttpStatusCode.OK
        }
    }

    test("tom gjennomføringstabell hvis ingen prismodell er konfigurert") {
        val okonomiConfig = OkonomiConfig(
            gyldigTilsagnPeriode = Tiltakskode.entries.associateWith {
                Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2030, 1, 1))
            },
            opprettKravPeriode = emptyMap(),
        )
        withTestApplication(ArrangorflateTestUtils.appConfig(oauth).copy(okonomi = okonomiConfig)) {
            val client = createClient {
                install(ContentNegotiation) {
                    json()
                }
            }

            val response = client.get("/api/arrangorflate/arrangor/$orgnr/gjennomforing/opprett-krav") {
                bearerAuth(oauth.issueToken(claims = mapOf("pid" to identMedTilgang.value)).serialize())
            }

            response.status shouldBe HttpStatusCode.OK
            val body = response.body<GjennomforingerTableResponse>()
            body.aktive.rows.size shouldBe 0
            body.historiske.rows.size shouldBe 0
        }
    }

    test("403 Forbidden ved opprettelse hvis konfigurasjon mangler") {
        val okonomiConfig = OkonomiConfig(
            gyldigTilsagnPeriode = Tiltakskode.entries.associateWith {
                Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2030, 1, 1))
            },
            opprettKravPeriode = emptyMap(),
        )
        val gjennomforingId = tilsagn.gjennomforingId
        val fil = "Innhold".toByteArray()
        val filnavn = "innhold.pdf"

        withTestApplication(ArrangorflateTestUtils.appConfig(oauth).copy(okonomi = okonomiConfig)) {
            val response =
                client.post("/api/arrangorflate/arrangor/$orgnr/gjennomforing/$gjennomforingId/opprett-krav") {
                    bearerAuth(oauth.issueToken(claims = mapOf("pid" to identMedTilgang.value)).serialize())
                    contentType(ContentType.MultiPart.FormData)
                    setBody(
                        MultiPartFormDataContent(
                            formData {
                                append("tilsagnId", tilsagn.id.toString())
                                append("periodeStart", tilsagn.periode.start.toString())
                                append("periodeSlutt", tilsagn.periode.slutt.toString())
                                append("belop", "1234")
                                append(
                                    "vedlegg",
                                    fil,
                                    Headers.build {
                                        append(
                                            HttpHeaders.ContentDisposition,
                                            "form-data; name=\"file\"; filename=\"$filnavn\"",
                                        )
                                        append(HttpHeaders.ContentType, ContentType.Application.Pdf.toString())
                                    },
                                )
                            },
                        ),
                    )
                }

            response.status shouldBe HttpStatusCode.Forbidden
        }
    }

    test("Timespris får liste av tilgjengelige perioder") {
        withTestApplication(ArrangorflateTestUtils.appConfig(oauth)) {
            val client = createClient {
                install(ContentNegotiation) {
                    json()
                }
            }
            val response =
                client.get("/api/arrangorflate/arrangor/$orgnr/gjennomforing/${oppfolgingGjennomforing.id}/opprett-krav/innsendingsinformasjon") {
                    bearerAuth(oauth.issueToken(claims = mapOf("pid" to identMedTilgang.value)).serialize())
                }

            response.status shouldBe HttpStatusCode.OK
            val data = response.body<OpprettKravInnsendingsInformasjon>()
            when (data.datoVelger) {
                is DatoVelger.DatoSelect ->
                    data.datoVelger.periodeForslag.isNotEmpty()
                is DatoVelger.DatoRange ->
                    fail { "Skal vise en liste av perioder for timespris innsending" }
            }
        }
    }

    test("Annen avtalt pris skal kunne velge fritt i datovelger") {
        withTestApplication(ArrangorflateTestUtils.appConfig(oauth)) {
            val client = createClient {
                install(ContentNegotiation) {
                    json()
                }
            }
            val response =
                client.get("/api/arrangorflate/arrangor/$orgnr/gjennomforing/${arrGjennomforing.id}/opprett-krav/innsendingsinformasjon") {
                    bearerAuth(oauth.issueToken(claims = mapOf("pid" to identMedTilgang.value)).serialize())
                }

            response.status shouldBe HttpStatusCode.OK
            val data = response.body<OpprettKravInnsendingsInformasjon>()
            when (data.datoVelger) {
                is DatoVelger.DatoSelect ->
                    fail { "Skal vise en liste av perioder for timespris innsending" }
                is DatoVelger.DatoRange ->
                    data.datoVelger.maksSluttdato shouldBe null
            }
        }
    }

    test("Investeringskrav skal bare kunne velge fra forrige utbetalingsperiode") {
        withTestApplication(ArrangorflateTestUtils.appConfig(oauth)) {
            val client = createClient {
                install(ContentNegotiation) {
                    json()
                }
            }
            val response =
                client.get("/api/arrangorflate/arrangor/$orgnr/gjennomforing/${aftGjennomforing.id}/opprett-krav/innsendingsinformasjon") {
                    bearerAuth(oauth.issueToken(claims = mapOf("pid" to identMedTilgang.value)).serialize())
                }

            response.status shouldBe HttpStatusCode.OK
            val data = response.body<OpprettKravInnsendingsInformasjon>()
            when (data.datoVelger) {
                is DatoVelger.DatoSelect ->
                    fail { "Skal vise en liste av perioder for timespris innsending" }
                is DatoVelger.DatoRange ->
                    data.datoVelger.maksSluttdato shouldBe LocalDate.now().minusDays(1) // Gårsdagen
            }
        }
    }
})
