package no.nav.mulighetsrommet.api.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.clients.person.Enhet
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus
import no.nav.mulighetsrommet.api.domain.dto.SanityResponse
import no.nav.mulighetsrommet.api.utils.TiltaksgjennomforingFilter
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingTilgjengelighetsstatus
import no.nav.mulighetsrommet.domain.dto.NavEnhet
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingAdminDto
import no.nav.mulighetsrommet.domain.dto.Tiltaksgjennomforingsstatus
import java.time.LocalDate
import java.util.*

class VeilederflateServiceTest : FunSpec({
    val sanityClient: SanityClient = mockk(relaxed = true)
    val brukerService: BrukerService = mockk(relaxed = true)
    val tiltaksgjennomforingService: TiltaksgjennomforingService = mockk(relaxed = true)
    val virksomhetService: VirksomhetService = mockk(relaxed = true)
    val tiltakstypeService: TiltakstypeService = mockk(relaxed = true)
    val navEnhetService: NavEnhetService = mockk()

    every { navEnhetService.hentOverorndetFylkesenhet("0430") } returns NavEnhetDbo(
        navn = "NAV Innlandet",
        enhetsnummer = "0400",
        status = NavEnhetStatus.AKTIV,
        type = Norg2Type.FYLKE,
        overordnetEnhet = null,
    )

    val sanityResult = SanityResponse.Result(
        ms = 12,
        query = "",
        result = Json.parseToJsonElement(
            """
        [
            {
                "_id": "8d8a5020-329d-4fbf-9eb2-20fc8a753a57",
                "tiltaksgjennomforingNavn": "Arbeidsrettet norsk for minoritetsspr\u00e5klige i Trondheim",
                "enheter": [],
                "oppstart": null,
                "oppstartsdato": null,
                "lokasjon": null,
                "tiltaksnummer": "2023#176408",
                "kontaktinfoArrangor": null,
                "tiltakstype": {
                    "tiltakstypeNavn": "Oppl\u00e6ring - Gruppe AMO"
                },
                "fylke": null
            },
            {
                "_id": "8d8a73bc-b661-4efd-90fc-2c59b258200e",
                "tiltaksgjennomforingNavn": "Oppf\u00f8lging Malvik",
                "tiltaksnummer": "2023#199282",
                "kontaktinfoArrangor": null,
                "lokasjon": "Oslo",
                "tiltakstype": {
                    "tiltakstypeNavn": "Oppf\u00f8lging"
                },
                "fylke": {
                    "_ref": "enhet.fylke.0400",
                    "_type": "reference"
                },
                "oppstart": null,
                "oppstartsdato": null,
                "enheter": []
            },
            {
                "_id": "f21d1e35-d63b-4de7-a0a5-589e57111527",
                "tiltaksgjennomforingNavn": "Oppf\u00f8lging for d\u00f8ve og personer med h\u00f8rselshemming - Innlandet",
                "oppstart": "dato",
                "lokasjon": null,
                "oppstartsdato": "2020-11-02",
                "tiltaksnummer": "2022#116075",
                "fylke": {
                    "_ref": "enhet.fylke.0400",
                    "_type": "reference"
                },
                "kontaktinfoArrangor": {
                    "selskapsnavn": "Signo Arbeid og deltakelse"
                },
                "tiltakstype": {
                    "tiltakstypeNavn": "Oppf\u00f8lging"
                },
                "enheter": [
                    {
                        "_ref": "enhet.lokal.0430",
                        "_type": "reference",
                        "_key": "21f3816b14ca"
                    }
                ]
            }
        ]
    """,
        ),
    )
    val tiltakstype =
        TiltaksgjennomforingAdminDto.Tiltakstype(id = UUID.randomUUID(), navn = "Tiltakstype", arenaKode = "TT")

    val dbGjennomforing = TiltaksgjennomforingAdminDto(
        id = UUID.randomUUID(),
        tiltakstype = tiltakstype,
        navn = "Navn",
        tiltaksnummer = null,
        arrangor = TiltaksgjennomforingAdminDto.Arrangor(
            organisasjonsnummer = "123456789",
            navn = null,
            kontaktperson = null,
            slettet = false,
        ),
        startDato = LocalDate.now(),
        sluttDato = null,
        arenaAnsvarligEnhet = null,
        status = Tiltaksgjennomforingsstatus.GJENNOMFORES,
        tilgjengelighet = TiltaksgjennomforingTilgjengelighetsstatus.LEDIG,
        estimertVentetid = null,
        antallPlasser = null,
        avtaleId = null,
        administrator = null,
        navEnheter = emptyList(),
        navRegion = null,
        sanityId = UUID.fromString("f21d1e35-d63b-4de7-a0a5-589e57111527"),
        oppstart = TiltaksgjennomforingOppstartstype.FELLES,
        opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE,
        stengtFra = null,
        stengtTil = null,
        kontaktpersoner = emptyList(),
        lokasjonArrangor = null,
    )

    test("Tom enhetsliste fra db overskriver ikke sanity enheter") {
        val fnr = "01010199999"
        val veilederFlateService = VeilederflateService(
            sanityClient,
            brukerService,
            tiltaksgjennomforingService,
            tiltakstypeService,
            navEnhetService,
        )
        every { tiltaksgjennomforingService.getBySanityIds(any()) } returns mapOf(
            UUID.fromString("f21d1e35-d63b-4de7-a0a5-589e57111527") to dbGjennomforing,
        )
        coEvery { virksomhetService.getOrSyncVirksomhet(any()) } returns null
        coEvery { brukerService.hentBrukerdata(any(), any()) } returns BrukerService.Brukerdata(
            fnr,
            geografiskEnhet = Enhet(navn = "A", enhetsnummer = "0430"),
            innsatsgruppe = null,
            oppfolgingsenhet = null,
            servicegruppe = null,
            fornavn = null,
            manuellStatus = null,
        )
        coEvery { sanityClient.query(any()) } returns sanityResult

        val gjennomforinger = veilederFlateService.hentTiltaksgjennomforingerForBrukerBasertPaEnhetOgFylke(
            fnr,
            "accessToken",
            TiltaksgjennomforingFilter(),
        )
        gjennomforinger.size shouldBe 2
        gjennomforinger.find { it._id == "f21d1e35-d63b-4de7-a0a5-589e57111527" }!!.enheter!!.size shouldBe 1
    }

    test("Filtrer på lokasjon") {
        val fnr = "01010199999"
        val veilederFlateService = VeilederflateService(
            sanityClient,
            brukerService,
            tiltaksgjennomforingService,
            tiltakstypeService,
            navEnhetService,
        )
        every { tiltaksgjennomforingService.getBySanityIds(any()) } returns mapOf(
            UUID.fromString("f21d1e35-d63b-4de7-a0a5-589e57111527") to dbGjennomforing.copy(lokasjonArrangor = "Oslo"),
        )
        coEvery { virksomhetService.getOrSyncVirksomhet(any()) } returns null
        coEvery { brukerService.hentBrukerdata(any(), any()) } returns BrukerService.Brukerdata(
            fnr,
            geografiskEnhet = Enhet(navn = "A", enhetsnummer = "0430"),
            innsatsgruppe = null,
            oppfolgingsenhet = null,
            servicegruppe = null,
            fornavn = null,
            manuellStatus = null,
        )
        coEvery { sanityClient.query(any()) } returns sanityResult

        val gjennomforinger = veilederFlateService.hentTiltaksgjennomforingerForBrukerBasertPaEnhetOgFylke(
            fnr,
            "accessToken",
            TiltaksgjennomforingFilter(lokasjoner = listOf("Oslo")),
        )
        gjennomforinger.size shouldBe 2
        gjennomforinger.find { it._id == "8d8a73bc-b661-4efd-90fc-2c59b258200e" }!!.lokasjon!! shouldBe "Oslo"
    }

    test("Samme enhet overskrevet fra admin flate skal fungere") {
        val fnr = "01010199999"
        val veilederFlateService = VeilederflateService(
            sanityClient,
            brukerService,
            tiltaksgjennomforingService,
            tiltakstypeService,
            navEnhetService,
        )
        every { tiltaksgjennomforingService.getBySanityIds(any()) } returns mapOf(
            UUID.fromString("f21d1e35-d63b-4de7-a0a5-589e57111527") to dbGjennomforing.copy(
                navEnheter = listOf(
                    NavEnhet(
                        enhetsnummer = "0430",
                        navn = "navn",
                    ),
                ),
            ),
        )
        coEvery { virksomhetService.getOrSyncVirksomhet(any()) } returns null
        coEvery { brukerService.hentBrukerdata(any(), any()) } returns BrukerService.Brukerdata(
            fnr,
            geografiskEnhet = Enhet(navn = "A", enhetsnummer = "0430"),
            innsatsgruppe = null,
            oppfolgingsenhet = null,
            servicegruppe = null,
            fornavn = null,
            manuellStatus = null,
        )
        coEvery { sanityClient.query(any()) } returns sanityResult

        val gjennomforinger = veilederFlateService.hentTiltaksgjennomforingerForBrukerBasertPaEnhetOgFylke(
            fnr,
            "accessToken",
            TiltaksgjennomforingFilter(),
        )
        gjennomforinger.size shouldBe 2
        gjennomforinger.find { it._id == "f21d1e35-d63b-4de7-a0a5-589e57111527" }!!.enheter!!.size shouldBe 1
    }

    test("Stengte filtreres vekk") {
        val fnr = "01010199999"
        val veilederFlateService = VeilederflateService(
            sanityClient,
            brukerService,
            tiltaksgjennomforingService,
            tiltakstypeService,
            navEnhetService,
        )
        every { tiltaksgjennomforingService.getBySanityIds(any()) } returns mapOf(
            UUID.fromString("f21d1e35-d63b-4de7-a0a5-589e57111527") to dbGjennomforing.copy(tilgjengelighet = TiltaksgjennomforingTilgjengelighetsstatus.STENGT),
        )
        coEvery { virksomhetService.getOrSyncVirksomhet(any()) } returns null
        coEvery { brukerService.hentBrukerdata(any(), any()) } returns BrukerService.Brukerdata(
            fnr,
            geografiskEnhet = Enhet(navn = "A", enhetsnummer = "0430"),
            innsatsgruppe = null,
            oppfolgingsenhet = null,
            servicegruppe = null,
            fornavn = null,
            manuellStatus = null,
        )
        coEvery { sanityClient.query(any()) } returns sanityResult

        val gjennomforinger = veilederFlateService.hentTiltaksgjennomforingerForBrukerBasertPaEnhetOgFylke(
            fnr,
            "accessToken",
            TiltaksgjennomforingFilter(),
        )
        gjennomforinger.size shouldBe 1
        gjennomforinger.find { it._id == "f21d1e35-d63b-4de7-a0a5-589e57111527" } shouldBe null
    }
})
