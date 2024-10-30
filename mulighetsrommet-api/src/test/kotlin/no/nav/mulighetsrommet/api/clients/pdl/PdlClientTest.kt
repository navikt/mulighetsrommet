package no.nav.mulighetsrommet.api.clients.pdl

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.ktor.client.engine.mock.*
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson
import no.nav.mulighetsrommet.tokenprovider.AccessType

class PdlClientTest : FunSpec({
    test("Missing errors is parsed ok") {
        val clientEngine = createMockEngine(
            "/graphql" to {
                respondJson(
                    """
                         {
                             "data": { "hentIdenter": { "identer": [] } }
                         }
                    """.trimIndent(),
                )
            },
        )
        val pdlClient = createPdlClient(clientEngine)

        val request = GraphqlRequest.HentHistoriskeIdenter(ident = PdlIdent("12345678910"), grupper = listOf())

        pdlClient.hentHistoriskeIdenter(request, AccessType.M2M).shouldBeRight(emptyList())
    }

    test("not_found gives NotFound") {
        val clientEngine = createMockEngine(
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
        )
        val pdlClient = createPdlClient(clientEngine)

        val request = GraphqlRequest.HentHistoriskeIdenter(ident = PdlIdent("12345678910"), grupper = listOf())

        pdlClient.hentHistoriskeIdenter(request, AccessType.M2M).shouldBeLeft(PdlError.NotFound)
    }

    test("håndterer errors og manglende data") {
        val clientEngine = createMockEngine(
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
        )
        val pdlClient = createPdlClient(clientEngine)

        val request = GraphqlRequest.HentHistoriskeIdenter(ident = PdlIdent("12345678910"), grupper = listOf())

        pdlClient.hentHistoriskeIdenter(request, AccessType.M2M).shouldBeLeft(PdlError.Error)
    }

    test("happy case hentIdenter") {
        val clientEngine = createMockEngine(
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
        )
        val pdlClient = createPdlClient(clientEngine)

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
        val clientEngine = createMockEngine(
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
        )
        val pdlClient = createPdlClient(clientEngine)

        pdlClient
            .hentPerson(PdlIdent("12345678910"), AccessType.M2M)
            .shouldBeRight(HentPersonResponse.Person(navn = listOf(PdlNavn(fornavn = "Ola", etternavn = "Normann"))))
    }

    test("happy case hentGeografiskTilknytning") {
        val clientEngine = createMockEngine(
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
        )
        val pdlClient = createPdlClient(clientEngine)

        pdlClient
            .hentGeografiskTilknytning(PdlIdent("12345678910"), AccessType.M2M)
            .shouldBeRight(GeografiskTilknytning.GtBydel(value = "030102"))
    }
})

private fun createPdlClient(clientEngine: MockEngine) = PdlClient(
    config = PdlClient.Config(baseUrl = "https://pdl.no"),
    tokenProvider = { "token" },
    clientEngine = clientEngine,
)
