package no.nav.mulighetsrommet.api.utbetaling

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import no.nav.mulighetsrommet.api.OkonomiConfig
import no.nav.mulighetsrommet.api.arrangorflate.api.GjennomforingerTableResponse
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.withTestApplication
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.time.LocalDate

class ArrangorflateOpprettKravRoutesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val deltaker = ArrangorflateTestUtils.createTestDeltaker()
    val tilsagn = ArrangorflateTestUtils.createTestTilsagn()

    val domain = ArrangorflateTestUtils.createTestDomain(
        deltaker = deltaker,
        tilsagn = tilsagn,
    )

    val oauth = MockOAuth2Server()
    val identMedTilgang = ArrangorflateTestUtils.identMedTilgang
    val underenhet = ArrangorflateTestUtils.underenhet
    val orgnr = underenhet.organisasjonsnummer.value

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
                                        append(HttpHeaders.ContentDisposition, "form-data; name=\"file\"; filename=\"$filnavn\"")
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
})
