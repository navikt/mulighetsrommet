package no.nav.mulighetsrommet.api.veilederflate.pdl

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import no.nav.mulighetsrommet.api.clients.pdl.PdlIdent
import no.nav.mulighetsrommet.api.clients.pdl.mockPdlClient
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson
import no.nav.mulighetsrommet.tokenprovider.AccessType

class HentBrukerPdlQueryTest : FunSpec({
    test("happy case hentPerson") {
        val clientEngine = createMockEngine {
            post("/graphql") {
                respondJson(
                    """
                        {
                            "data": {
                                "hentPerson": {
                                    "navn": [{ "fornavn": "Ola" }]
                                },
                                "hentGeografiskTilknytning": {
                                    "gtType": "BYDEL",
                                    "gtLand": null,
                                    "gtKommune": null,
                                    "gtBydel": "030102"
                                }
                            }
                        }
                    """.trimIndent(),
                )
            }
        }
        val query = HentBrukerPdlQuery(mockPdlClient(clientEngine))

        query.hentBruker(PdlIdent("12345678910"), AccessType.M2M).shouldBeRight(
            HentBrukerResponse(
                "Ola",
                GeografiskTilknytning.GtBydel(value = "030102"),
            ),
        )
    }
})
