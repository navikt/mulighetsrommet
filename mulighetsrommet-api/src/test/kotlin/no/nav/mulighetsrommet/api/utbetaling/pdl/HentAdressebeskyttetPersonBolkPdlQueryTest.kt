package no.nav.mulighetsrommet.api.utbetaling.pdl

import arrow.core.nonEmptySetOf
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.content.*
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.clients.pdl.*
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson

class HentAdressebeskyttetPersonBolkPdlQueryTest : FunSpec({
    test("happy case hentPersonBolk") {
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
                                ]
                            }
                        }
                    """.trimIndent(),
                )
            }
        }

        val query = HentAdressebeskyttetPersonBolkPdlQuery(mockPdlClient(clientEngine))

        query.hentPersonBolk(identer) shouldBeRight mapOf(
            PdlIdent("12345678910") to PdlPerson(
                navn = "Normann, Ola",
                gradering = PdlGradering.STRENGT_FORTROLIG,
                foedselsdato = null,
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
            }
        }

        val query = HentAdressebeskyttetPersonBolkPdlQuery(mockPdlClient(clientEngine))

        query.hentPersonBolk(nonEmptySetOf(PdlIdent("12345678910"))) shouldBeRight mapOf(
            PdlIdent("12345678910") to PdlPerson(
                navn = "Normann, Ola",
                gradering = PdlGradering.UGRADERT,
                foedselsdato = null,
            ),
        )
    }
})
