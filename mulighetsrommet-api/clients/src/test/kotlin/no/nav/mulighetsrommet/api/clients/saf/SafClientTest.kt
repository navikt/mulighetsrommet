package no.nav.mulighetsrommet.api.clients.saf

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson
import no.nav.mulighetsrommet.tokenprovider.AccessType

class SafClientTest : FunSpec({
    test("hentDokument returnerer filinnhold og sender bearer-token") {
        val clientEngine = createMockEngine {
            get("/rest/hentdokument/453857496/123456789/ARKIV") { request ->
                request.headers[HttpHeaders.Authorization] shouldBe "Bearer token"
                respond(
                    content = byteArrayOf(37, 80, 68, 70, 0, -1),
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Pdf.toString()),
                )
            }
        }
        val saf = mockSafClient(clientEngine)

        saf.hentDokument("453857496", "123456789", "ARKIV", AccessType.M2M)
            .shouldBeRight().toList() shouldBe listOf<Byte>(37, 80, 68, 70, 0, -1)
    }

    test("hentDokument bruker valgt dokumentvariant") {
        val clientEngine = createMockEngine {
            get("/rest/hentdokument/453857496/123456789/ORIGINAL") {
                respond(
                    content = byteArrayOf(1, 2, 3),
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.OctetStream.toString()),
                )
            }
        }
        val saf = mockSafClient(clientEngine)

        saf.hentDokument("453857496", "123456789", "ORIGINAL", AccessType.M2M)
            .shouldBeRight().toList() shouldBe listOf<Byte>(1, 2, 3)
    }

    for ((status, forventetFeil) in listOf(
        HttpStatusCode.NotFound to SafError.NotFound,
        HttpStatusCode.Unauthorized to SafError.Error,
        HttpStatusCode.Forbidden to SafError.Forbidden,
        HttpStatusCode.InternalServerError to SafError.Error,
    )) {
        test("hentDokument gir $forventetFeil ved HTTP ${status.value}") {
            val clientEngine = createMockEngine {
                get("/rest/hentdokument/453857496/123456789/ARKIV") {
                    respond(content = "Dokumentet kunne ikke hentes", status = status)
                }
            }
            val saf = mockSafClient(clientEngine)

            saf.hentDokument("453857496", "123456789", "ARKIV", AccessType.M2M).shouldBeLeft(forventetFeil)
        }
    }

    test("journalpost som finnes gir Right") {
        val clientEngine = createMockEngine {
            post("/graphql") {
                respondJson(
                    """
                        {
                            "data": {
                                "journalpost": {
                                    "journalpostId": "453857496",
                                    "bruker": { "id": "12345678910", "type": "FNR" }
                                }
                            }
                        }
                    """.trimIndent(),
                )
            }
        }
        val saf = mockSafClient(clientEngine)

        saf.hentBrukerForJournalpost("453857496", AccessType.M2M)
            .shouldBeRight(
                SafJournalpost(
                    journalpostId = "453857496",
                    bruker = SafBruker(id = "12345678910", type = SafBrukerIdType.FNR),
                ),
            )

        val request = Json.decodeFromString<GraphqlRequest<GraphqlRequest.JournalpostId>>(
            (clientEngine.requestHistory.single().body as TextContent).text,
        )
        request.variables.journalpostId shouldBe "453857496"
        request.query shouldContain "bruker"
        request.query shouldNotContain "dokumenter"
    }

    test("dokumenter og varianter returneres fra dokumentoppslag") {
        val clientEngine = createMockEngine {
            post("/graphql") {
                respondJson(
                    """
                        {
                            "data": {
                                "journalpost": {
                                    "journalpostId": "453857496",
                                    "dokumenter": [
                                        {
                                            "dokumentInfoId": "123456789",
                                            "tittel": "Faktura",
                                            "dokumentvarianter": [
                                                {
                                                    "variantformat": "ARKIV",
                                                    "filtype": "PDF",
                                                    "saksbehandlerHarTilgang": true
                                                },
                                                {
                                                    "variantformat": "ORIGINAL",
                                                    "filtype": "DOCX",
                                                    "saksbehandlerHarTilgang": false
                                                }
                                            ]
                                        }
                                    ]
                                }
                            }
                        }
                    """.trimIndent(),
                )
            }
        }
        val saf = mockSafClient(clientEngine)

        saf.hentDokumenterForJournalpost("453857496", AccessType.M2M).shouldBeRight(
            SafJournalpost(
                journalpostId = "453857496",
                dokumenter = listOf(
                    SafDokument(
                        dokumentInfoId = "123456789",
                        tittel = "Faktura",
                        dokumentvarianter = listOf(
                            SafDokumentvariant("ARKIV", "PDF", true),
                            SafDokumentvariant("ORIGINAL", "DOCX", false),
                        ),
                    ),
                ),
            ),
        )

        val request = Json.decodeFromString<GraphqlRequest<GraphqlRequest.JournalpostId>>(
            (clientEngine.requestHistory.single().body as TextContent).text,
        )
        request.variables.journalpostId shouldBe "453857496"
        request.query shouldContain "dokumenter"
        request.query shouldContain "dokumentvarianter"
        request.query shouldNotContain "bruker"
    }

    for ((navn, hentJournalpost) in listOf(
        "bruker" to SafClient::hentBrukerForJournalpost,
        "dokumenter" to SafClient::hentDokumenterForJournalpost,
    )) {
        context(navn) {
            test("not_found gir NotFound") {
                val clientEngine = createMockEngine {
                    post("/graphql") {
                        respondJson(
                            """
                                {
                                    "data": { "journalpost": null },
                                    "errors": [
                                        { "extensions": { "code": "not_found" } }
                                    ]
                                }
                            """.trimIndent(),
                        )
                    }
                }
                val saf = mockSafClient(clientEngine)

                hentJournalpost(saf, "finnes-ikke", AccessType.M2M).shouldBeLeft(SafError.NotFound)
            }

            test("null journalpost uten errors gir NotFound") {
                val clientEngine = createMockEngine {
                    post("/graphql") {
                        respondJson(
                            """
                                {
                                    "data": { "journalpost": null }
                                }
                            """.trimIndent(),
                        )
                    }
                }
                val saf = mockSafClient(clientEngine)

                hentJournalpost(saf, "finnes-ikke", AccessType.M2M).shouldBeLeft(SafError.NotFound)
            }

            test("andre feil gir Error") {
                val clientEngine = createMockEngine {
                    post("/graphql") {
                        respondJson(
                            """
                                {
                                    "data": { "journalpost": null },
                                    "errors": [
                                        { "extensions": { "code": "forbidden" } }
                                    ]
                                }
                            """.trimIndent(),
                        )
                    }
                }
                val saf = mockSafClient(clientEngine)

                hentJournalpost(saf, "453857496", AccessType.M2M).shouldBeLeft(SafError.Error)
            }

            test("http-feil gir Error") {
                val clientEngine = createMockEngine {
                    post("/graphql") {
                        respondJson("{}", status = HttpStatusCode.InternalServerError)
                    }
                }
                val saf = mockSafClient(clientEngine)

                hentJournalpost(saf, "453857496", AccessType.M2M).shouldBeLeft(SafError.Error)
            }

            test("ugyldig respons gir Error") {
                val clientEngine = createMockEngine {
                    post("/graphql") {
                        respondJson("ikke json")
                    }
                }
                val saf = mockSafClient(clientEngine)

                hentJournalpost(saf, "453857496", AccessType.M2M).shouldBeLeft(SafError.Error)
            }
        }
    }
})
