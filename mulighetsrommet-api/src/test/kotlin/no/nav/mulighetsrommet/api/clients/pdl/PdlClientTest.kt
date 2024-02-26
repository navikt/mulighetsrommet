package no.nav.mulighetsrommet.api.clients.pdl

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import no.nav.mulighetsrommet.ktor.createMockEngine

class PdlClientTest : FunSpec({
    test("Missing errors is parsed ok") {
        val pdlClient = PdlClient(
            baseUrl = "https://pdl.no",
            tokenProvider = { "token" },
            clientEngine = createMockEngine(
                "/graphql" to {
                    respond(
                        content = """
                            {
                                "data": { "hentIdenter": { "identer": [] } }
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString())),
                    )
                },
            ),
        )

        pdlClient.hentIdenter("12345678910").shouldBeRight(emptyList())
    }

    test("not_found gives NotFound") {
        val pdlClient = PdlClient(
            baseUrl = "https://pdl.no",
            tokenProvider = { "token" },
            clientEngine = createMockEngine(
                "/graphql" to {
                    respond(
                        content = """
                            {
                                "data": { "hentIdenter": null },
                                "errors": [
                                    { "extensions": { "code": "not_found" } },
                                    { "extensions": { "code": "another_error" } }
                                ]
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString())),
                    )
                },
            ),
        )

        pdlClient.hentIdenter("12345678910").shouldBeLeft(PdlError.NotFound)
    }
})
