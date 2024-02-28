package no.nav.mulighetsrommet.api.clients.pdl

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
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

        pdlClient.hentIdenter("12345678910", null).shouldBeRight(emptyList())
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

        pdlClient.hentIdenter("12345678910", null).shouldBeLeft(PdlError.NotFound)
    }

    test("happy case") {
        val pdlClient = PdlClient(
            baseUrl = "https://pdl.no",
            tokenProvider = { "token" },
            clientEngine = createMockEngine(
                "/graphql" to {
                    respond(
                        content = """
                            {
                                "data": {
                                    "hentIdenter": {
                                        "identer": [
                                            {
                                                "ident": "12345678910",
                                                "gruppe": "FOLKEREGISTERIDENT",
                                                "historisk": false
                                            },
                                            {
                                                "ident": "123",
                                                "gruppe": "AKTORID",
                                                "historisk": true
                                            },
                                            {
                                                "ident": "99999999999",
                                                "gruppe": "NPID",
                                                "historisk": true
                                            }
                                        ]
                                    }
                                },
                                "errors": []
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString())),
                    )
                },
            ),
        )

        val identer = pdlClient.hentIdenter("12345678910", null).shouldBeRight()
        identer shouldContainExactlyInAnyOrder listOf(
            IdentInformasjon(
                ident = "12345678910",
                gruppe = IdentGruppe.FOLKEREGISTERIDENT,
                historisk = false,
            ),
            IdentInformasjon(
                ident = "123",
                gruppe = IdentGruppe.AKTORID,
                historisk = true,
            ),
            IdentInformasjon(
                ident = "99999999999",
                gruppe = IdentGruppe.NPID,
                historisk = true,
            ),
        )
    }
})
