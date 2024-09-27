package no.nav.mulighetsrommet.api.clients.cms

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.domain.dto.SanityResponse
import no.nav.mulighetsrommet.api.services.cms.CacheUsage
import no.nav.mulighetsrommet.api.services.cms.SanityService
import java.util.*

class CmsClientTest : FunSpec({
    val sanityClient: SanityClient = mockk(relaxed = true)

    beforeEach {
        clearMocks(sanityClient)
    }

    val enkelAmoSanityId = UUID.randomUUID()
    val arbeidstreningSanityId = UUID.randomUUID()

    val sanityResult = SanityResponse.Result(
        ms = 12,
        query = "",
        result = Json.parseToJsonElement(
            """
        [
            {
                "_id": "6c64a4bd-2ae1-4aee-ad19-716884bf3b5e",
                "tiltaksgjennomforingNavn": "Enkel AMO",
                "oppstart": null,
                "oppstartsdato": null,
                "stedForGjennomforing": null,
                "tiltaksnummer": "2023#176408",
                "tiltakstype": {
                    "_id": "$enkelAmoSanityId",
                    "tiltakstypeNavn": "Arbeidsmarkedsopplæring (enkeltplass)",
                    "innsatsgrupper": ["SITUASJONSBESTEMT_INNSATS", "SPESIELT_TILPASSET_INNSATS", "GRADERT_VARIG_TILPASSET_INNSATS", "VARIG_TILPASSET_INNSATS"]
                },
                "fylke": "0300",
                "enheter": [],
                "arrangor": {
                    "_id": "${UUID.randomUUID()}",
                    "navn": "Fretex",
                    "organisasjonsnummer": null,
                    "kontaktpersoner": [
                        {
                            "_id": "${UUID.randomUUID()}",
                            "navn": "Donald",
                            "telefon": "12341234",
                            "epost": "donald@fretex.no",
                            "beskrivelse": "Daglig leder"
                        }
                    ]
                }
            },
            {
                "_id": "f21d1e35-d63b-4de7-a0a5-589e57111527",
                "tiltaksgjennomforingNavn": "Arbeidstrening Innlandet",
                "tiltaksnummer": null,
                "stedForGjennomforing": "Innlandet",
                "tiltakstype": {
                    "_id": "$arbeidstreningSanityId",
                    "tiltakstypeNavn": "Arbeidstrening",
                    "innsatsgrupper": ["SITUASJONSBESTEMT_INNSATS", "SPESIELT_TILPASSET_INNSATS", "GRADERT_VARIG_TILPASSET_INNSATS", "VARIG_TILPASSET_INNSATS"]
                },
                "fylke": "0400",
                "oppstart": null,
                "oppstartsdato": null,
                "enheter": null
            },
            {
                "_id": "82cebdb9-24ef-4f6d-b6b2-6ed45c67d3b6",
                "tiltaksgjennomforingNavn": "Arbeidstrening",
                "oppstart": "dato",
                "stedForGjennomforing": null,
                "oppstartsdato": "2020-11-02",
                "tiltaksnummer": null,
                "fylke": "0400",
                "tiltakstype": {
                    "_id": "$arbeidstreningSanityId",
                    "tiltakstypeNavn": "Arbeidstrening",
                    "innsatsgrupper": ["SITUASJONSBESTEMT_INNSATS", "SPESIELT_TILPASSET_INNSATS", "GRADERT_VARIG_TILPASSET_INNSATS", "VARIG_TILPASSET_INNSATS"]
                },
                "enheter": ["0501"],
                "faneinnhold": { "forHvemInfoboks": "infoboks" }
            }
        ]
    """,
        ),
    )

    test("Med UseCache kalles sanity kun én gang") {
        val sanityService = SanityService(sanityClient)

        coEvery { sanityClient.query(any(), any()) } returns sanityResult

        sanityService.getAllTiltak(
            search = null,
            cacheUsage = CacheUsage.UseCache,
        ) shouldHaveSize 3

        sanityService.getAllTiltak(
            search = null,
            cacheUsage = CacheUsage.UseCache,
        ) shouldHaveSize 3

        coVerify(exactly = 1) {
            sanityClient.query(any(), any())
        }
    }
})
