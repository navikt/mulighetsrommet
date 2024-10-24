package no.nav.mulighetsrommet.altinn

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson

class AltinnClientTest : FunSpec({
    val altinnResponse = """
			{
  "isError": false,
  "hierarki": [
    {
      "orgnr": "314048814",
      "altinn3Tilganger": [],
      "altinn2Tilganger": [],
      "underenheter": [
        {
          "orgnr": "211267232",
          "altinn3Tilganger": [
            "tiltak-arrangor-refusjon"
          ],
          "altinn2Tilganger": [],
          "underenheter": [],
          "navn": "UEMOSJONELL KREATIV TIGER AS",
          "organisasjonsform": "BEDR",
          "orgNr": "211267232",
          "name": "UEMOSJONELL KREATIV TIGER AS",
          "organizationForm": "BEDR"
        }
      ],
      "navn": "UEMOSJONELL KREATIV TIGER AS",
      "organisasjonsform": "AS",
      "orgNr": "314048814",
      "name": "UEMOSJONELL KREATIV TIGER AS",
      "organizationForm": "AS"
    },
    {
      "orgnr": "312899485",
      "altinn3Tilganger": [
        "tiltak-arrangor-refusjon"
      ],
      "altinn2Tilganger": [],
      "underenheter": [],
      "navn": "FRYKTLØS OPPSTEMT STRUTS LTD",
      "organisasjonsform": "NUF",
      "orgNr": "312899485",
      "name": "FRYKTLØS OPPSTEMT STRUTS LTD",
      "organizationForm": "NUF"
    }
  ],
  "orgNrTilTilganger": {
    "211267232": [
      "tiltak-arrangor-refusjon"
    ]
  },
  "tilgangTilOrgNr": {
    "tiltak-arrangor-refusjon": [
      "211267232"
    ]
  }
}
    """.trimIndent()

    test("hentAlleOrganisasjoner 1 tilgang - kun et kall til Altinn") {
        val altinnClient = AltinnClient(
            "http://arbeidsgiver-altinn-tilgang.fager",
            tokenProvider = { "token" },
            createMockEngine(
                "/altinn-tilganger" to {
                    respondJson(altinnResponse)
                },
            ),
        )

        val organisasjoner = altinnClient.hentRettigheter("token")

        organisasjoner shouldHaveSize 2
    }
})
