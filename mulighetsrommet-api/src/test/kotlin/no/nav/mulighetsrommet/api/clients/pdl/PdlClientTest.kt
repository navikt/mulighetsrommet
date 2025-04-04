package no.nav.mulighetsrommet.api.clients.pdl

import arrow.core.Either
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson
import no.nav.mulighetsrommet.tokenprovider.AccessType

class PdlClientTest : FunSpec({
    @Serializable
    data class HentPerson(
        val hentPerson: PdlPerson?,
    )

    class HentFornavnMockQuery(private val pdl: PdlClient) {
        suspend fun hentFornavn(variables: GraphqlRequest.Ident): Either<PdlError, String?> {
            val request = GraphqlRequest(
                query = $$"""
                query($ident: ID!) {
                    hentPerson(ident: $ident) {
                        navn(historikk: false) {
                            fornavn
                        }
                    }
                }
                """.trimIndent(),
                variables = variables,
            )
            return pdl.graphqlRequest<GraphqlRequest.Ident, HentPerson>(request, AccessType.M2M).map { response ->
                response.hentPerson?.navn?.first()?.fornavn
            }
        }
    }

    test("Missing errors is parsed ok") {
        val clientEngine = createMockEngine {
            post("/graphql") {
                respondJson(
                    """
                        {
                            "data": {
                                "hentPerson": {
                                    "navn": [{ "fornavn": "Ola", "mellomnavn": null, "etternavn": null }]
                                }
                            }
                        }
                    """.trimIndent(),
                )
            }
        }
        val query = HentFornavnMockQuery(mockPdlClient(clientEngine))

        val request = GraphqlRequest.Ident(ident = PdlIdent("12345678910"))

        query.hentFornavn(request).shouldBeRight("Ola")
    }

    test("not_found gives NotFound") {
        val clientEngine = createMockEngine {
            post("/graphql") {
                respondJson(
                    """
                        {
                            "data": { "hentPerson": null },
                            "errors": [
                                { "extensions": { "code": "not_found" } },
                                { "extensions": { "code": "bad_request" } }
                            ]
                        }
                    """.trimIndent(),
                )
            }
        }
        val query = HentFornavnMockQuery(mockPdlClient(clientEngine))

        val request = GraphqlRequest.Ident(ident = PdlIdent("12345678910"))

        query.hentFornavn(request).shouldBeLeft(PdlError.NotFound)
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
        val query = HentFornavnMockQuery(mockPdlClient(clientEngine))

        val request = GraphqlRequest.Ident(ident = PdlIdent("12345678910"))

        query.hentFornavn(request).shouldBeLeft(PdlError.Error)
    }
})
