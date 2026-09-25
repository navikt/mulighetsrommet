package no.nav.mulighetsrommet.api.clients.saf

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.ktor.http.HttpStatusCode
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson
import no.nav.mulighetsrommet.tokenprovider.AccessType

class SafClientTest : FunSpec({
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

        saf.hentJournalpost("453857496", AccessType.M2M)
            .shouldBeRight(
                SafJournalpost(
                    journalpostId = "453857496",
                    bruker = SafBruker(id = "12345678910", type = SafBrukerIdType.FNR),
                ),
            )
    }

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

        saf.hentJournalpost("finnes-ikke", AccessType.M2M).shouldBeLeft(SafError.NotFound)
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

        saf.hentJournalpost("finnes-ikke", AccessType.M2M).shouldBeLeft(SafError.NotFound)
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

        saf.hentJournalpost("453857496", AccessType.M2M).shouldBeLeft(SafError.Error)
    }

    test("http-feil gir Error") {
        val clientEngine = createMockEngine {
            post("/graphql") {
                respondJson("{}", status = HttpStatusCode.InternalServerError)
            }
        }
        val saf = mockSafClient(clientEngine)

        saf.hentJournalpost("453857496", AccessType.M2M).shouldBeLeft(SafError.Error)
    }
})
