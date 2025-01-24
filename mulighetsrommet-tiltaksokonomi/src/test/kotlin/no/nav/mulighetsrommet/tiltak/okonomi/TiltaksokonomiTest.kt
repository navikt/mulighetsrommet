package no.nav.mulighetsrommet.tiltak.okonomi

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.resources.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.tiltak.okonomi.oebs.OebsBestilling
import no.nav.mulighetsrommet.tiltak.okonomi.oebs.OebsBestillingArrangor
import no.nav.mulighetsrommet.tiltak.okonomi.oebs.OkonomiPart
import no.nav.mulighetsrommet.tiltak.okonomi.oebs.OpprettOebsBestilling
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.intellij.lang.annotations.Language
import java.time.LocalDate

class TiltaksokonomiTest : FunSpec({
    val oauth = MockOAuth2Server()

    beforeSpec {
        oauth.start()
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

                val response = client.get(Bestilling.Id(id = "T-123"))

                response.status shouldBe HttpStatusCode.Unauthorized
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

        test("opprett bestilling") {
            val mockEngine = createMockEngine(
                "http://oebs-tiltak-api/api/v1/oebs/bestilling" to { respondOk() },
                "http://brreg/enheter/123456789" to {
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
                },
            )

            withTestApplication(oauth, mockEngine) {
                val client = createClient {
                    install(Resources)
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val response = client.post(Bestilling()) {
                    bearerAuth(oauth.issueToken().serialize())
                    contentType(ContentType.Application.Json)
                    setBody(
                        OpprettOebsBestilling(
                            tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
                            arrangor = OebsBestillingArrangor(
                                hovedenhet = Organisasjonsnummer("123456789"),
                                underenhet = Organisasjonsnummer("234567891"),
                            ),
                            bestillingsnummer = "T-123",
                            avtalenummer = "2025/1234",
                            belop = 1000,
                            opprettetAv = OkonomiPart.Tiltaksadministrasjon,
                            opprettetTidspunkt = LocalDate.of(2025, 1, 1).atStartOfDay(),
                            besluttetAv = OkonomiPart.Tiltaksadministrasjon,
                            besluttetTidspunkt = LocalDate.of(2025, 1, 1).atStartOfDay(),
                            periode = Periode(start = LocalDate.of(2025, 1, 1), slutt = LocalDate.of(2025, 2, 1)),
                            kostnadssted = NavEnhetNummer("0400"),
                        ),
                    )
                }

                response.status shouldBe HttpStatusCode.OK

                response.body<OebsBestilling>() shouldBe OebsBestilling(
                    bestillingsnummer = "T-123",
                    status = OebsBestilling.Status.BEHANDLET,
                )
            }
        }

        xtest("hente status på bestilling") {
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

                response.status shouldBe HttpStatusCode.OK

                response.body<OebsBestilling>() shouldBe OebsBestilling(
                    bestillingsnummer = "T-123",
                    status = OebsBestilling.Status.BEHANDLET,
                )
            }
        }
    }
})
