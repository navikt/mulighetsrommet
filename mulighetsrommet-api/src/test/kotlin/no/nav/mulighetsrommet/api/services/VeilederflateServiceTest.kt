package no.nav.mulighetsrommet.api.services

import arrow.core.nonEmptyListOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus
import no.nav.mulighetsrommet.api.domain.dto.SanityResponse
import no.nav.mulighetsrommet.api.domain.dto.VeilederflateArrangor
import no.nav.mulighetsrommet.api.domain.dto.VeilederflateTiltaksgjennomforing
import no.nav.mulighetsrommet.api.domain.dto.VeilederflateTiltakstype
import no.nav.mulighetsrommet.api.routes.v1.ApentForInnsok
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import no.nav.mulighetsrommet.domain.dto.Faneinnhold
import java.time.LocalDate
import java.util.*

class VeilederflateServiceTest : FunSpec({
    val sanityClient: SanityClient = mockk(relaxed = true)
    val tiltaksgjennomforingService: TiltaksgjennomforingService = mockk(relaxed = true)
    val virksomhetService: VirksomhetService = mockk(relaxed = true)
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
                    "tiltakstypeNavn": "Opplæring - Gruppe AMO"
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
                    "tiltakstypeNavn": "Individuelt Tiltak"
                },
                "fylke": "0400",
                "oppstart": null,
                "oppstartsdato": null,
                "enheter": []
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
                    "tiltakstypeNavn": "Oppfølging"
                },
                "enheter": ["0430"],
                "faneinnhold": { "forHvemInfoboks": "infoboks" }
            }
        ]
    """,
        ),
    )

    val dbGjennomforing = VeilederflateTiltaksgjennomforing(
        sanityId = "f21d1e35-d63b-4de7-a0a5-589e57111527",
        tiltakstype = VeilederflateTiltakstype(
            sanityId = UUID.randomUUID().toString(),
            navn = "Oppfølging",
        ),
        navn = "Navn",
        tiltaksnummer = null,
        arrangor = VeilederflateArrangor(
            organisasjonsnummer = "123456789",
            selskapsnavn = null,
            kontaktperson = null,
        ),
        oppstartsdato = LocalDate.now(),
        sluttdato = null,
        apentForInnsok = true,
        enheter = emptyList(),
        fylke = "0400",
        oppstart = TiltaksgjennomforingOppstartstype.FELLES,
        stengtFra = null,
        stengtTil = null,
        stedForGjennomforing = null,
        faneinnhold = null,
        beskrivelse = null,
        kontaktinfoTiltaksansvarlige = emptyList(),
    )

    test("Samme enhet overskrevet fra admin flate skal fungere") {
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
        } returns listOf(
            dbGjennomforing.copy(enheter = listOf("0430")),
        )
        coEvery { virksomhetService.getOrSyncVirksomhet(any()) } returns null
        coEvery { sanityClient.query(any()) } returns sanityResult

        val gjennomforinger = veilederFlateService.hentTiltaksgjennomforinger(
            enheter = nonEmptyListOf("0430"),
        )
        gjennomforinger.size shouldBe 2
        gjennomforinger.find { it.sanityId == "f21d1e35-d63b-4de7-a0a5-589e57111527" }!!.enheter!!.size shouldBe 1
    }

    test("Bruker db faneinnhold hvis det finnes") {
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
        } returns listOf(
            dbGjennomforing.copy(faneinnhold = Faneinnhold(forHvemInfoboks = "123")),
        )
        coEvery { virksomhetService.getOrSyncVirksomhet(any()) } returns null
        coEvery { sanityClient.query(any()) } returns sanityResult

        val gjennomforinger = veilederFlateService.hentTiltaksgjennomforinger(
            enheter = nonEmptyListOf("0430"),
        )
        gjennomforinger.size shouldBe 2
        gjennomforinger.find { it.sanityId == "f21d1e35-d63b-4de7-a0a5-589e57111527" }!!
            .faneinnhold!!.forHvemInfoboks shouldBe "123"
    }

    test("Returnerer korrekt gjennomføringer for brukers enheter") {
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
        } returns listOf(
            dbGjennomforing.copy(enheter = listOf("0455")),
        )
        coEvery { virksomhetService.getOrSyncVirksomhet(any()) } returns null
        coEvery { sanityClient.query(any()) } returns sanityResult

        veilederFlateService.hentTiltaksgjennomforinger(
            enheter = nonEmptyListOf("0430", "0455"),
        ) shouldHaveSize 2
    }

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
        coEvery { sanityClient.query(any()) } returns sanityResult

        veilederFlateService.hentTiltaksgjennomforinger(
            enheter = nonEmptyListOf("0430"),
            apentForInnsok = ApentForInnsok.APENT,
        ) shouldHaveSize 2

        veilederFlateService.hentTiltaksgjennomforinger(
            enheter = nonEmptyListOf("0430"),
            apentForInnsok = ApentForInnsok.APENT_ELLER_STENGT,
        ) shouldHaveSize 2

        veilederFlateService.hentTiltaksgjennomforinger(
            enheter = nonEmptyListOf("0430"),
            apentForInnsok = ApentForInnsok.STENGT,
        ) shouldHaveSize 0
    }
})
