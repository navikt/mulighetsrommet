package no.nav.mulighetsrommet.api.services

import arrow.core.nonEmptyListOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus
import no.nav.mulighetsrommet.api.domain.dto.SanityResponse
import no.nav.mulighetsrommet.api.routes.v1.ApentForInnsok
import no.nav.mulighetsrommet.domain.dto.Innsatsgruppe
import java.util.*

class VeilederflateServiceTest : FunSpec({
    val sanityClient: SanityClient = mockk(relaxed = true)
    val tiltaksgjennomforingService: TiltaksgjennomforingService = mockk(relaxed = true)
    val tiltakstypeService: TiltakstypeService = mockk(relaxed = true)
    val navEnhetService: NavEnhetService = mockk()

    every { navEnhetService.hentOverordnetFylkesenhet("0430") } returns NavEnhetDbo(
        navn = "NAV Innlandet",
        enhetsnummer = "0400",
        status = NavEnhetStatus.AKTIV,
        type = Norg2Type.FYLKE,
        overordnetEnhet = null,
    )

    val sanityGjennomforingInnlandet = UUID.fromString("8d8a73bc-b661-4efd-90fc-2c59b258200e")
    val sanityGjennomforingStorElvdal = UUID.fromString("f21d1e35-d63b-4de7-a0a5-589e57111527")

    beforeEach {
        clearMocks(sanityClient)
    }

    val sanityResult = SanityResponse.Result(
        ms = 12,
        query = "",
        result = Json.parseToJsonElement(
            """
        [
            {
                "_id": "8d8a5020-329d-4fbf-9eb2-20fc8a753a57",
                "tiltaksgjennomforingNavn": "Arbeidsrettet norsk for minoritetsspråklige i Trondheim",
                "enheter": [],
                "oppstart": null,
                "oppstartsdato": null,
                "stedForGjennomforing": null,
                "tiltaksnummer": "2023#176408",
                "tiltakstype": {
                    "_id": "${UUID.randomUUID()}",
                    "tiltakstypeNavn": "Opplæring - Gruppe AMO",
                    "innsatsgrupper": ["SITUASJONSBESTEMT_INNSATS", "SPESIELT_TILPASSET_INNSATS", "GRADERT_VARIG_TILPASSET_INNSATS", "VARIG_TILPASSET_INNSATS"]
                },
                "fylke": null
            },
            {
                "_id": "$sanityGjennomforingInnlandet",
                "tiltaksgjennomforingNavn": "Oppfølging Malvik",
                "tiltaksnummer": "2023#199282",
                "stedForGjennomforing": "Oslo",
                "tiltakstype": {
                    "_id": "${UUID.randomUUID()}",
                    "tiltakstypeNavn": "Individuelt Tiltak",
                    "innsatsgrupper": ["SITUASJONSBESTEMT_INNSATS", "SPESIELT_TILPASSET_INNSATS", "GRADERT_VARIG_TILPASSET_INNSATS", "VARIG_TILPASSET_INNSATS"]
                },
                "fylke": "0400",
                "oppstart": null,
                "oppstartsdato": null,
                "enheter": null
            },
            {
                "_id": "$sanityGjennomforingStorElvdal",
                "tiltaksgjennomforingNavn": "Oppfølging for døve og personer med hørselshemming - Innlandet",
                "oppstart": "dato",
                "stedForGjennomforing": null,
                "oppstartsdato": "2020-11-02",
                "tiltaksnummer": "2022#116075",
                "fylke": "0400",
                "tiltakstype": {
                    "_id": "${UUID.randomUUID()}",
                    "tiltakstypeNavn": "Oppfølging",
                    "innsatsgrupper": ["SITUASJONSBESTEMT_INNSATS", "SPESIELT_TILPASSET_INNSATS", "GRADERT_VARIG_TILPASSET_INNSATS", "VARIG_TILPASSET_INNSATS"]
                },
                "enheter": ["0430"],
                "faneinnhold": { "forHvemInfoboks": "infoboks" }
            }
        ]
    """,
        ),
    )

    test("henter ikke gjennomføringer fra Sanity når filter for 'Åpent for innsøk' er STENGT") {
        val veilederFlateService = VeilederflateService(
            sanityClient,
            tiltaksgjennomforingService,
            tiltakstypeService,
            navEnhetService,
        )
        every {
            tiltaksgjennomforingService.getAllVeilederflateTiltaksgjennomforing(
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns listOf()

        coEvery { sanityClient.query(any(), any()) } returns sanityResult

        veilederFlateService.hentTiltaksgjennomforinger(
            enheter = nonEmptyListOf("0430"),
            apentForInnsok = ApentForInnsok.APENT,
            innsatsgruppe = Innsatsgruppe.VARIG_TILPASSET_INNSATS,
            cacheUsage = CacheUsage.NoCache,
        ) shouldHaveSize 2

        veilederFlateService.hentTiltaksgjennomforinger(
            enheter = nonEmptyListOf("0430"),
            apentForInnsok = ApentForInnsok.APENT_ELLER_STENGT,
            innsatsgruppe = Innsatsgruppe.VARIG_TILPASSET_INNSATS,
            cacheUsage = CacheUsage.NoCache,
        ) shouldHaveSize 2

        veilederFlateService.hentTiltaksgjennomforinger(
            enheter = nonEmptyListOf("0430"),
            apentForInnsok = ApentForInnsok.STENGT,
            innsatsgruppe = Innsatsgruppe.VARIG_TILPASSET_INNSATS,
            cacheUsage = CacheUsage.NoCache,
        ) shouldHaveSize 0
    }

    test("Med UseCache kalles sanity kun én gang") {
        val veilederFlateService = VeilederflateService(
            sanityClient,
            tiltaksgjennomforingService,
            tiltakstypeService,
            navEnhetService,
        )
        every {
            tiltaksgjennomforingService.getAllVeilederflateTiltaksgjennomforing(
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns listOf()

        coEvery { sanityClient.query(any(), any()) } returns sanityResult

        veilederFlateService.hentTiltaksgjennomforinger(
            enheter = nonEmptyListOf("0430"),
            apentForInnsok = ApentForInnsok.APENT,
            innsatsgruppe = Innsatsgruppe.VARIG_TILPASSET_INNSATS,
            cacheUsage = CacheUsage.UseCache,
        ) shouldHaveSize 2

        veilederFlateService.hentTiltaksgjennomforinger(
            enheter = nonEmptyListOf("0430"),
            apentForInnsok = ApentForInnsok.APENT,
            innsatsgruppe = Innsatsgruppe.VARIG_TILPASSET_INNSATS,
            cacheUsage = CacheUsage.UseCache,
        ) shouldHaveSize 2

        coVerify(exactly = 1) {
            sanityClient.query(any(), any())
        }
    }
})
