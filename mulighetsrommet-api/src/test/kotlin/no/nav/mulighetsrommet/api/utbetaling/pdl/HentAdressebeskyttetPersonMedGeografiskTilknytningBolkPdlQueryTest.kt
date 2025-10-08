package no.nav.mulighetsrommet.api.utbetaling.pdl

import arrow.core.nonEmptySetOf
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.content.TextContent
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.clients.pdl.GeografiskTilknytning
import no.nav.mulighetsrommet.api.clients.pdl.GraphqlRequest
import no.nav.mulighetsrommet.api.clients.pdl.PdlGradering
import no.nav.mulighetsrommet.api.clients.pdl.PdlIdent
import no.nav.mulighetsrommet.api.clients.pdl.mockPdlClient
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson
import no.nav.mulighetsrommet.tokenprovider.AccessType

class HentAdressebeskyttetPersonMedGeografiskTilknytningBolkPdlQueryTest : FunSpec({
    test("happy case hentPersonOgGeografisktilknytningBolk") {
        val identer = nonEmptySetOf(PdlIdent("12345678910"), PdlIdent("12345678911"), PdlIdent("test"))

        val clientEngine = createMockEngine {
            post("/graphql") {
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
                                             ],
                                             "adressebeskyttelse": [
                                                 {
                                                     "gradering": "STRENGT_FORTROLIG"
                                                 }
                                             ],
                                             "foedselsdato": [
                                                 {
                                                     "foedselsaar": 1980,
                                                     "foedselsdato": null
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
                                ],
                                "hentGeografiskTilknytningBolk": [
                                {
                                    "ident": "12345678910",
                                    "geografiskTilknytning": {
                                      "gtType": "BYDEL",
                                      "gtLand": null,
                                      "gtKommune": null,
                                      "gtBydel": "030102"
                                    },
                                    "code": "ok"
                                },
                                {
                                    "ident": "12345678911",
                                    "geografiskTilknytning": null,
                                    "code": "not_found"
                                },
                                {
                                    "ident": "test",
                                    "geografiskTilknytning": null,
                                    "code": "bad_request"
                                }
                        ]
                            }
                        }
                    """.trimIndent(),
                )
            }
        }

        val query = HentAdressebeskyttetPersonMedGeografiskTilknytningBolkPdlQuery(mockPdlClient(clientEngine))

        query.hentPersonOgGeografiskTilknytningBolk(identer, AccessType.M2M) shouldBeRight mapOf(
            PdlIdent("12345678910") to Pair(
                PdlPerson(
                    navn = "Normann, Ola",
                    gradering = PdlGradering.STRENGT_FORTROLIG,
                    foedselsdato = null,
                ),
                GeografiskTilknytning.GtBydel(
                    value = "030102",
                ),
            ),
        )
    }

    test("tolker manglende gradering som UGRADERT") {
        val clientEngine = createMockEngine {
            post("/graphql") {
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
                                             ],
                                             "adressebeskyttelse": [
                                                 {
                                                     "gradering": null
                                                 }
                                             ],
                                             "foedselsdato": [
                                                 {
                                                     "foedselsaar": 1980,
                                                     "foedselsdato": null
                                                 }
                                             ]
                                        },
                                        "code": "ok"
                                    }
                                ],
                                "hentGeografiskTilknytningBolk": [
                                {
                                    "ident": "12345678910",
                                    "geografiskTilknytning": {
                                      "gtType": "BYDEL",
                                      "gtLand": null,
                                      "gtKommune": null,
                                      "gtBydel": "030102"
                                    },
                                    "code": "ok"
                                }
                        ]
                            }
                        }
                    """.trimIndent(),
                )
            }
        }

        val query = HentAdressebeskyttetPersonMedGeografiskTilknytningBolkPdlQuery(mockPdlClient(clientEngine))

        query.hentPersonOgGeografiskTilknytningBolk(
            nonEmptySetOf(PdlIdent("12345678910")),
            AccessType.M2M,
        ) shouldBeRight mapOf(
            PdlIdent("12345678910") to Pair(
                PdlPerson(
                    navn = "Normann, Ola",
                    gradering = PdlGradering.UGRADERT,
                    foedselsdato = null,
                ),
                GeografiskTilknytning.GtBydel(
                    value = "030102",
                ),
            ),
        )
    }

    test("Håndterer manglende geografisk tilknytning") {
        val clientEngine = createMockEngine {
            post("/graphql") {
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
                                             ],
                                             "adressebeskyttelse": [
                                                 {
                                                     "gradering": null
                                                 }
                                             ],
                                             "foedselsdato": [
                                                 {
                                                     "foedselsaar": 1980,
                                                     "foedselsdato": null
                                                 }
                                             ]
                                        },
                                        "code": "ok"
                                    }
                                ],
                                "hentGeografiskTilknytningBolk": [
                                {
                                    "ident": "12345678910",
                                    "geografiskTilknytning": null
                                    "code": "ok"
                                }
                        ]
                            }
                        }
                    """.trimIndent(),
                )
            }
        }

        val query = HentAdressebeskyttetPersonMedGeografiskTilknytningBolkPdlQuery(mockPdlClient(clientEngine))

        query.hentPersonOgGeografiskTilknytningBolk(
            nonEmptySetOf(PdlIdent("12345678910")),
            AccessType.M2M,
        ) shouldBeRight mapOf(
            PdlIdent("12345678910") to Pair(
                PdlPerson(
                    navn = "Normann, Ola",
                    gradering = PdlGradering.UGRADERT,
                    foedselsdato = null,
                ),
                GeografiskTilknytning.GtUdefinert,
            ),

        )
    }
})
