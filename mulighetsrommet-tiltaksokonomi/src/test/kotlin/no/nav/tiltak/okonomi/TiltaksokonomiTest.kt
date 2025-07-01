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
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.tiltak.okonomi.api.Bestilling
import no.nav.tiltak.okonomi.api.Faktura
import no.nav.tiltak.okonomi.db.OkonomiDatabase
import no.nav.tiltak.okonomi.model.OebsKontering
import no.nav.tiltak.okonomi.oebs.OebsPoApClient
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

        test("behandle bestilling") {
            val mockEngine = createMockEngine {
                mockBrregHovedenhet()

                post(OebsPoApClient.BESTILLING_ENDPOINT) { respondOk() }
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
                            arrangor = Organisasjonsnummer("234567891"),
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
                        status = BestillingStatusType.SENDT,
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
                "forretningsadresse": {
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

    get("https://data.brreg.no/enhetsregisteret/api/enheter/234567891") {
        @Language("json")
        val brregResponse = """
            {
                "organisasjonsnummer": "234567891",
                "navn": "Tiltaksarrangør Underenhet",
                "organisasjonsform": {
                    "kode": "BEDR",
                    "beskrivelse": "Underenhet til næringsdrivende og offentlig forvaltning"
                },
                "overordnetEnhet": "123456789"
            }
        """.trimIndent()
        respondJson(brregResponse)
    }
}
