package no.nav.mulighetsrommet.api.clients.pdl

import arrow.core.nonEmptySetOf
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.content.*
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.okonomi.refusjon.HentAdressebeskyttetPersonBolkPdlQuery
import no.nav.mulighetsrommet.api.okonomi.refusjon.HentPersonBolkResponse
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson

class HentPersonBolkPdlQueryTest : FunSpec({
    test("happy case hentPersonBolk") {
        val identer = nonEmptySetOf(PdlIdent("12345678910"), PdlIdent("12345678911"), PdlIdent("test"))

        val pdl = PdlClient(
            config = PdlClient.Config(baseUrl = "https://pdl.no"),
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
                                                 ],
                                                 "adressebeskyttelse": {
                                                     "gradering": null
                                                 },
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
                },
            ),
        )

        val query = HentAdressebeskyttetPersonBolkPdlQuery(pdl)

        val response = query.hentPersonBolk(identer).shouldBeRight()

        response shouldBe mapOf(
            PdlIdent("12345678910") to HentPersonBolkResponse.Person(
                navn = listOf(
                    PdlNavn(fornavn = "Ola", etternavn = "Normann"),
                ),
                adressebeskyttelse = HentPersonBolkResponse.Adressebeskyttelse(
                    gradering = null,
                ),
                foedselsdato = listOf(
                    HentPersonBolkResponse.Foedselsdato(
                        foedselsaar = 1980,
                        foedselsdato = null,
                    ),
                ),
            ),
        )
    }
})
