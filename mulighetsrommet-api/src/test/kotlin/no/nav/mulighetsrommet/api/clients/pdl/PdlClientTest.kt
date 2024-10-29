package no.nav.mulighetsrommet.api.clients.pdl

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.http.content.*
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson
import no.nav.mulighetsrommet.tokenprovider.AccessType

class PdlClientTest : FunSpec({
    test("Missing errors is parsed ok") {
        val pdlClient = PdlClient(
            baseUrl = "https://pdl.no",
            tokenProvider = { "token" },
            clientEngine = createMockEngine(
                "/graphql" to {
                    respondJson(
                        """
                            {
                                "data": { "hentIdenter": { "identer": [] } }
                            }
                        """.trimIndent(),
                    )
                },
            ),
        )

        val request = GraphqlRequest.HentHistoriskeIdenter(ident = PdlIdent("12345678910"), grupper = listOf())
        pdlClient.hentHistoriskeIdenter(request, AccessType.M2M).shouldBeRight(emptyList())
    }

    test("not_found gives NotFound") {
        val pdlClient = PdlClient(
            baseUrl = "https://pdl.no",
            tokenProvider = { "token" },
            clientEngine = createMockEngine(
                "/graphql" to {
                    respondJson(
                        """
                            {
                                "data": { "hentIdenter": null },
                                "errors": [
                                    { "extensions": { "code": "not_found" } },
                                    { "extensions": { "code": "bad_request" } }
                                ]
                            }
                        """.trimIndent(),
                    )
                },
            ),
        )

        val request = GraphqlRequest.HentHistoriskeIdenter(ident = PdlIdent("12345678910"), grupper = listOf())
        pdlClient.hentHistoriskeIdenter(request, AccessType.M2M).shouldBeLeft(PdlError.NotFound)
    }

    test("håndterer errors og manglende data") {
        val pdlClient = PdlClient(
            baseUrl = "https://pdl.no",
            tokenProvider = { "token" },
            clientEngine = createMockEngine(
                "/graphql" to {
                    respondJson(
                        """
                            {
                                "data": null,
                                "errors": [
                                    { "extensions": { "code": "bad_request" } }
                                ]
                            }
                        """.trimIndent(),
                    )
                },
            ),
        )

        val request = GraphqlRequest.HentHistoriskeIdenter(ident = PdlIdent("12345678910"), grupper = listOf())
        pdlClient.hentHistoriskeIdenter(request, AccessType.M2M).shouldBeLeft(PdlError.Error)
    }

    test("happy case hentIdenter") {
        val pdlClient = PdlClient(
            baseUrl = "https://pdl.no",
            tokenProvider = { "token" },
            clientEngine = createMockEngine(
                "/graphql" to {
                    respondJson(
                        """
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
                    )
                },
            ),
        )

        val request = GraphqlRequest.HentHistoriskeIdenter(
            ident = PdlIdent("12345678910"),
            grupper = IdentGruppe.entries,
        )
        val identer = pdlClient.hentHistoriskeIdenter(request, AccessType.M2M).shouldBeRight()
        identer shouldContainExactlyInAnyOrder listOf(
            IdentInformasjon(
                ident = PdlIdent("12345678910"),
                gruppe = IdentGruppe.FOLKEREGISTERIDENT,
                historisk = false,
            ),
            IdentInformasjon(
                ident = PdlIdent("123"),
                gruppe = IdentGruppe.AKTORID,
                historisk = true,
            ),
            IdentInformasjon(
                ident = PdlIdent("99999999999"),
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
                    respondJson(
                        """
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
                    )
                },
            ),
        )

        val person = pdlClient.hentPerson(PdlIdent("12345678910"), AccessType.M2M).shouldBeRight()
        person shouldBe PdlPerson(navn = listOf(PdlPerson.PdlNavn(fornavn = "Ola", etternavn = "Normann")))
    }

    test("happy case hentGeografiskTilknytning") {
        val pdlClient = PdlClient(
            baseUrl = "https://pdl.no",
            tokenProvider = { "token" },
            clientEngine = createMockEngine(
                "/graphql" to {
                    respondJson(
                        """
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
                    )
                },
            ),
        )

        val geografiskTilknytning =
            pdlClient.hentGeografiskTilknytning(PdlIdent("12345678910"), AccessType.M2M).shouldBeRight()
        geografiskTilknytning shouldBe GeografiskTilknytning.GtBydel(value = "030102")
    }

    test("happy case hentPersonBolk") {
        val identer = setOf(PdlIdent("12345678910"), PdlIdent("12345678911"), PdlIdent("test"))

        val pdlClient = PdlClient(
            baseUrl = "https://pdl.no",
            tokenProvider = { "token" },
            clientEngine = createMockEngine(
                "/graphql" to {

                    val body = Json.decodeFromString<GraphqlRequest<GraphqlRequest.Identer>>(
                        (it.body as TextContent).text,
                    )
                    body.variables.identer shouldBe identer

                    respondJson(
                        """
                            {
                                "data": {
                                    "hentPersonBolk": [
                                        {
                                            "ident": "12345678910",
                                            "person": {
                                                 "navn": [
                                                     {
                                                         "fornavn": "Ola",
                                                         "mellomnavn": null,
                                                         "etternavn": "Normann"
                                                     }
                                                 ]
                                            },
                                            "code": "ok"
                                        },
                                        {
                                            "ident": "12345678911",
                                            "person": null,
                                            "code": "not_found"
                                        },
                                        {
                                            "ident": "test",
                                            "person": null,
                                            "code": "bad_request"
                                        }
                                    ]
                                }
                            }
                        """.trimIndent(),
                    )
                },
            ),
        )

        val response = pdlClient.hentPersonBolk(identer).shouldBeRight()
        response shouldBe mapOf(
            PdlIdent("12345678910") to PdlPerson(
                navn = listOf(
                    PdlPerson.PdlNavn(
                        fornavn = "Ola",
                        etternavn = "Normann",
                    ),
                ),
            ),
        )
    }
})
