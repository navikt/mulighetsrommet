package no.nav.mulighetsrommet.api.clients.pdl

import arrow.core.nonEmptySetOf
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.content.*
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson

class HentPersonBolkPdlQueryTest : FunSpec({
    test("happy case hentPersonBolk") {
        val identer = nonEmptySetOf(PdlIdent("12345678910"), PdlIdent("12345678911"), PdlIdent("test"))

        val pdl = PdlClient(
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

        val query = HentPersonBolkPdlQuery(pdl)

        val response = query.hentPersonBolk(identer).shouldBeRight()

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
