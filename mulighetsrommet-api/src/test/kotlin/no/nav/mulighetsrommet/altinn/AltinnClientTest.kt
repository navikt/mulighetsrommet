package no.nav.mulighetsrommet.altinn

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson
import no.nav.mulighetsrommet.model.NorskIdent

class AltinnClientTest : FunSpec({
    val altinnResponse = """
			[
				{
					"name": "LAGSPORT PLUTSELIG",
					"organizationNumber": "123456789",
					"type": "Person",
					"authorizedResources": [],
					"subunits": []
				},
				{
					"name": "NONFIGURATIV KOMFORTABEL HUND DA",
					"type": "Organization",
					"organizationNumber": "999987004",
					"authorizedResources": [],
					"subunits": [
						{
							"name": "UEMOSJONELL KREATIV TIGER AS",
							"type": "Organization",
							"organizationNumber": "211267232",
							"authorizedResources": ["nav_tiltaksarrangor_be-om-utbetaling"],
							"subunits": []
						}
					]
				},
				{
					"name": "FRYKTLØS OPPSTEMT STRUTS LTD",
					"type": "Organization",
					"organizationNumber": "312899485",
					"authorizedResources": ["nav_tiltaksarrangor_be-om-utbetaling"],
					"subunits": []
				}
			]
    """.trimIndent()

    test("hentAlleOrganisasjoner 1 tilgang - kun et kall til Altinn") {
        val altinnClient = AltinnClient(
            "https://altinn.no",
            tokenProvider = { "token" },
            createMockEngine(
                "/accessmanagement/api/v1/resourceowner/authorizedparties?includeAltinn2=true" to {
                    respondJson(altinnResponse)
                },
            ),
        )

        val norskIdent = NorskIdent("12345678901")
        val organisasjoner = altinnClient.hentRettigheter(norskIdent)

        organisasjoner shouldHaveSize 2
    }
})
