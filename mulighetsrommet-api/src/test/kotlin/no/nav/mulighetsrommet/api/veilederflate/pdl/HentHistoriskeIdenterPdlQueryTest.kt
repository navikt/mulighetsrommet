package no.nav.mulighetsrommet.api.veilederflate.pdl

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import no.nav.mulighetsrommet.api.clients.pdl.*
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson
import no.nav.mulighetsrommet.tokenprovider.AccessType

class HentHistoriskeIdenterPdlQueryTest : FunSpec({
    test("Missing errors is parsed ok") {
        val clientEngine = createMockEngine {
            post("/graphql") {
                respondJson(
                    """
                         {
                             "data": { "hentIdenter": { "identer": [] } }
                         }
                    """.trimIndent(),
                )
            }
        }
        val query = HentHistoriskeIdenterPdlQuery(mockPdlClient(clientEngine))

        val request = GraphqlRequest.HentHistoriskeIdenter(ident = PdlIdent("12345678910"), grupper = listOf())

        query.hentHistoriskeIdenter(request, AccessType.M2M).shouldBeRight(emptyList())
    }

    test("not_found gives NotFound") {
        val clientEngine = createMockEngine {
            post("/graphql") {
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
            }
        }
        val query = HentHistoriskeIdenterPdlQuery(mockPdlClient(clientEngine))

        val request = GraphqlRequest.HentHistoriskeIdenter(ident = PdlIdent("12345678910"), grupper = listOf())

        query.hentHistoriskeIdenter(request, AccessType.M2M).shouldBeLeft(PdlError.NotFound)
    }

    test("håndterer errors og manglende data") {
        val clientEngine = createMockEngine {
            post("/graphql") {
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
            }
        }
        val query = HentHistoriskeIdenterPdlQuery(mockPdlClient(clientEngine))

        val request = GraphqlRequest.HentHistoriskeIdenter(ident = PdlIdent("12345678910"), grupper = listOf())

        query.hentHistoriskeIdenter(request, AccessType.M2M).shouldBeLeft(PdlError.Error)
    }

    test("happy case hentIdenter") {
        val clientEngine = createMockEngine {
            post("/graphql") {
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
            }
        }
        val query = HentHistoriskeIdenterPdlQuery(mockPdlClient(clientEngine))

        val request = GraphqlRequest.HentHistoriskeIdenter(
            ident = PdlIdent("12345678910"),
            grupper = IdentGruppe.entries,
        )

        query.hentHistoriskeIdenter(request, AccessType.M2M) shouldBeRight listOf(
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
})
