package no.nav.mulighetsrommet.api.clients.pdl

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import no.nav.mulighetsrommet.api.clients.AccessType
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

        pdlClient.hentIdenter("12345678910", AccessType.M2M).shouldBeRight(emptyList())
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

        pdlClient.hentIdenter("12345678910", AccessType.M2M).shouldBeLeft(PdlError.NotFound)
    }

    test("happy case hentIdenter") {
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

        val identer = pdlClient.hentIdenter("12345678910", AccessType.M2M).shouldBeRight()
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

    test("happy case hentPerson") {
        val pdlClient = PdlClient(
            baseUrl = "https://pdl.no",
            tokenProvider = { "token" },
            clientEngine = createMockEngine(
                "/graphql" to {
                    respond(
                        content = """
                            {
                                "data": {
                                    "hentPerson": {
                                        "navn": [
                                            {
                                                "fornavn": "Ola",
                                                "mellomnavn": null,
                                                "etternavn": "Normann"
                                            }
                                      ]
                                    }
                                }
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString())),
                    )
                },
            ),
        )

        val person = pdlClient.hentPerson("12345678910", AccessType.M2M).shouldBeRight()
        person shouldBe PdlPerson(navn = listOf(PdlPerson.PdlNavn(fornavn = "Ola", etternavn = "Normann")))
    }

    test("happy case hentGeografiskTilknytning") {
        val pdlClient = PdlClient(
            baseUrl = "https://pdl.no",
            tokenProvider = { "token" },
            clientEngine = createMockEngine(
                "/graphql" to {
                    respond(
                        content = """
                            {
                                "data": {
                                    "hentGeografiskTilknytning":{
                                        "gtType": "BYDEL",
                                        "gtLand": null,
                                        "gtKommune": null,
                                        "gtBydel": "030102"
                                    }
                                }
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString())),
                    )
                },
            ),
        )

        val geografiskTilknytning = pdlClient.hentGeografiskTilknytning("12345678910", AccessType.M2M).shouldBeRight()
        geografiskTilknytning shouldBe GeografiskTilknytning.GtBydel(value = "030102")
    }
})
