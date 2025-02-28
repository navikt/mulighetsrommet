package no.nav.tiltak.okonomi

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.resources.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.ktor.MockEngineBuilder
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson
import no.nav.mulighetsrommet.model.*
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.tiltak.okonomi.api.*
import no.nav.tiltak.okonomi.db.OkonomiDatabase
import no.nav.tiltak.okonomi.model.BestillingStatusType
import no.nav.tiltak.okonomi.model.FakturaStatusType
import no.nav.tiltak.okonomi.model.OebsKontering
import org.intellij.lang.annotations.Language
import java.time.LocalDate

class TiltaksokonomiTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    val oauth = MockOAuth2Server()

    beforeSpec {
        oauth.start()

        val db = OkonomiDatabase(database.db)
        initializeData(db)
    }

    afterSpec {
        oauth.shutdown()
    }

    context("bestilling") {
        test("unauthorized når token mangler") {
            val mockEngine = createMockEngine()

            withTestApplication(oauth, mockEngine) {
                val client = createClient {
                    install(Resources)
                    install(ContentNegotiation) {
                        json()
                    }
                }

                client.post(Bestilling()).status shouldBe HttpStatusCode.Unauthorized
                client.post(Bestilling.Id.Status(Bestilling.Id(id = "A-1"))).status shouldBe HttpStatusCode.Unauthorized
                client.get(Bestilling.Id(id = "A-1")).status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("no content når bestilling ikke finnes") {
            val mockEngine = createMockEngine()

            withTestApplication(oauth, mockEngine) {
                val client = createClient {
                    install(Resources)
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val response = client.get(Bestilling.Id(id = "T-123")) {
                    bearerAuth(oauth.issueToken().serialize())
                }

                response.status shouldBe HttpStatusCode.NoContent
            }
        }

        test("behandle og annuller bestilling") {
            val mockEngine = createMockEngine {
                mockBrregHovedenhet()

                post("/api/v1/tilsagn") { respondOk() }
            }

            withTestApplication(oauth, mockEngine) {
                val client = createClient {
                    install(Resources)
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val bestillingsnummer = "A-1"

                client.post(Bestilling()) {
                    bearerAuth(oauth.issueToken().serialize())
                    contentType(ContentType.Application.Json)
                    setBody(
                        OpprettBestilling(
                            bestillingsnummer = bestillingsnummer,
                            tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
                            tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
                            arrangor = OpprettBestilling.Arrangor(
                                hovedenhet = Organisasjonsnummer("123456789"),
                                underenhet = Organisasjonsnummer("234567891"),
                            ),
                            avtalenummer = "2025/1234",
                            belop = 1000,
                            behandletAv = OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON),
                            behandletTidspunkt = LocalDate.of(2025, 1, 1).atStartOfDay(),
                            besluttetAv = OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON),
                            besluttetTidspunkt = LocalDate.of(2025, 1, 1).atStartOfDay(),
                            periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                            kostnadssted = NavEnhetNummer("0400"),
                        ),
                    )
                }.also {
                    it.status shouldBe HttpStatusCode.Created
                }

                client.get(Bestilling.Id(id = bestillingsnummer)) {
                    bearerAuth(oauth.issueToken().serialize())
                }.also {
                    it.status shouldBe HttpStatusCode.OK
                    it.body<BestillingStatus>() shouldBe BestillingStatus(
                        bestillingsnummer = bestillingsnummer,
                        status = BestillingStatusType.BESTILT,
                    )
                }

                client.post(Bestilling.Id.Status(Bestilling.Id(id = bestillingsnummer))) {
                    bearerAuth(oauth.issueToken().serialize())
                    contentType(ContentType.Application.Json)
                    setBody(SetBestillingStatus(status = BestillingStatusType.ANNULLERT))
                }.also {
                    it.status shouldBe HttpStatusCode.OK
                }

                client.get(Bestilling.Id(id = bestillingsnummer)) {
                    bearerAuth(oauth.issueToken().serialize())
                }.also {
                    it.status shouldBe HttpStatusCode.OK
                    it.body<BestillingStatus>() shouldBe BestillingStatus(
                        bestillingsnummer = bestillingsnummer,
                        status = BestillingStatusType.ANNULLERT,
                    )
                }
            }
        }
    }

    context("faktura") {
        test("unauthorized når token mangler") {
            val mockEngine = createMockEngine()

            withTestApplication(oauth, mockEngine) {
                val client = createClient {
                    install(Resources)
                    install(ContentNegotiation) {
                        json()
                    }
                }

                client.post(Faktura()).status shouldBe HttpStatusCode.Unauthorized
                client.get(Faktura.Id(id = "A-1")).status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("behandle og send faktura") {
            val mockEngine = createMockEngine {
                mockBrregHovedenhet()

                post("/api/v1/tilsagn") { respondOk() }

                post("/api/v1/refusjonskrav") { respondOk() }
            }

            withTestApplication(oauth, mockEngine) {
                val client = createClient {
                    install(Resources)
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val bestillingsnummer = "A-2"

                client.post(Bestilling()) {
                    bearerAuth(oauth.issueToken().serialize())
                    contentType(ContentType.Application.Json)
                    setBody(
                        OpprettBestilling(
                            bestillingsnummer = bestillingsnummer,
                            tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
                            tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
                            arrangor = OpprettBestilling.Arrangor(
                                hovedenhet = Organisasjonsnummer("123456789"),
                                underenhet = Organisasjonsnummer("234567891"),
                            ),
                            avtalenummer = null,
                            belop = 1000,
                            behandletAv = OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON),
                            behandletTidspunkt = LocalDate.of(2025, 1, 1).atStartOfDay(),
                            besluttetAv = OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON),
                            besluttetTidspunkt = LocalDate.of(2025, 1, 1).atStartOfDay(),
                            periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                            kostnadssted = NavEnhetNummer("0400"),
                        ),
                    )
                }.also {
                    it.status shouldBe HttpStatusCode.Created
                }

                val fakturanummer = "1"

                client.post(Faktura()) {
                    bearerAuth(oauth.issueToken().serialize())
                    contentType(ContentType.Application.Json)
                    setBody(
                        OpprettFaktura(
                            fakturanummer = fakturanummer,
                            bestillingsnummer = bestillingsnummer,
                            betalingsinformasjon = OpprettFaktura.Betalingsinformasjon(
                                kontonummer = Kontonummer("12345678901"),
                                kid = null,
                            ),
                            belop = 1000,
                            periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                            behandletAv = OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON),
                            behandletTidspunkt = LocalDate.of(2025, 1, 1).atStartOfDay(),
                            besluttetAv = OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON),
                            besluttetTidspunkt = LocalDate.of(2025, 1, 1).atStartOfDay(),
                        ),
                    )
                }.also {
                    it.status shouldBe HttpStatusCode.Created
                }

                client.get(Faktura.Id(id = fakturanummer)) {
                    bearerAuth(oauth.issueToken().serialize())
                }.also {
                    it.status shouldBe HttpStatusCode.OK
                    it.body<FakturaStatus>() shouldBe FakturaStatus(
                        fakturanummer = fakturanummer,
                        status = FakturaStatusType.UTBETALT,
                    )
                }
            }
        }
    }
})

private fun initializeData(db: OkonomiDatabase) = db.session {
    queries.kontering.insertKontering(
        OebsKontering(
            tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
            tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
            periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2099, 1, 1)),
            statligRegnskapskonto = "12345678901",
            statligArtskonto = "23456789012",
        ),
    )
}

private fun MockEngineBuilder.mockBrregHovedenhet() {
    get("https://data.brreg.no/enhetsregisteret/api/enheter/123456789") {
        @Language("json")
        val brregResponse = """
            {
                "organisasjonsnummer": "123456789",
                "navn": "Tiltaksarrangør AS",
                "organisasjonsform": {
                    "kode": "AS",
                    "beskrivelse": "Aksjeselskap"
                },
                "postadresse": {
                    "land": "Norge",
                    "landkode": "NO",
                    "postnummer": "0170",
                    "poststed": "OSLO",
                    "adresse": ["Gateveien 1"],
                    "kommune": "OSLO",
                    "kommunenummer": "0301"
                }
            }
        """.trimIndent()
        respondJson(brregResponse)
    }
}
