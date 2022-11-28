package no.nav.mulighetsrommet.api.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.mulighetsrommet.api.clients.arena_ords_proxy.ArenaOrdsProxyClient
import no.nav.mulighetsrommet.api.clients.enhetsregister.AmtEnhetsregisterClient
import no.nav.mulighetsrommet.api.domain.ArrangorDTO
import no.nav.mulighetsrommet.api.domain.VirksomhetDTO

class ArrangorServiceTest : FunSpec({

    val arenaOrdsProxyClient: ArenaOrdsProxyClient = mockk()
    val amtEnhetsregister: AmtEnhetsregisterClient = mockk()

    val arrangorService = ArrangorService(arenaOrdsProxyClient, amtEnhetsregister)

    beforeSpec {
        coEvery { arenaOrdsProxyClient.hentArbeidsgiver(1) } returns ArrangorDTO(
            virksomhetsnummer = "111",
            organisasjonsnummerMorselskap = "456"
        )
        coEvery { arenaOrdsProxyClient.hentArbeidsgiver(2) } returns ArrangorDTO(
            virksomhetsnummer = "222",
            organisasjonsnummerMorselskap = "456"
        )

        coEvery { amtEnhetsregister.hentVirksomhet(111) } returns VirksomhetDTO(
            organisasjonsnummer = "789",
            navn = "Bedrift 1",
            overordnetEnhetOrganisasjonsnummer = "1011",
            overordnetEnhetNavn = "Overordnetbedrift 1"
        )
        coEvery { amtEnhetsregister.hentVirksomhet(222) } returns VirksomhetDTO(
            organisasjonsnummer = "7891",
            navn = "Bedrift 2",
            overordnetEnhetOrganisasjonsnummer = "1011",
            overordnetEnhetNavn = "Overordnetbedrift 2"
        )
    }

    test("henter navn på arrangør basert på virksomhetsnummer tilhørende arrangør id") {
        arrangorService.hentArrangornavn("1") shouldBe "Overordnetbedrift 1"
        arrangorService.hentArrangornavn("2") shouldBe "Overordnetbedrift 2"
    }

    test("arrangør navn blir cachet basert på arrangør id") {
        arrangorService.hentArrangornavn("1")
        arrangorService.hentArrangornavn("2")
        arrangorService.hentArrangornavn("2")
        arrangorService.hentArrangornavn("2")
        arrangorService.hentArrangornavn("1")

        coVerify(exactly = 2) { amtEnhetsregister.hentVirksomhet(any()) }
        coVerify(exactly = 2) { arenaOrdsProxyClient.hentArbeidsgiver(any()) }
    }
})
