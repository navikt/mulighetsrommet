package no.nav.mulighetsrommet.api.services

import io.kotest.common.runBlocking
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.mulighetsrommet.api.clients.amtenhetsregister.AmtEnhetsregisterClient
import no.nav.mulighetsrommet.api.clients.arenaordsproxy.ArenaOrdsProxyClient
import no.nav.mulighetsrommet.api.domain.ArbeidsgiverDTO
import no.nav.mulighetsrommet.api.domain.VirksomhetDTO

class ArrangorServiceTest : FunSpec({

    val arenaOrdsProxyClient: ArenaOrdsProxyClient = mockk()
    val amtEnhetsregister: AmtEnhetsregisterClient = mockk()

    val arrangorService = ArrangorService(arenaOrdsProxyClient, amtEnhetsregister)

    beforeSpec {
        every { runBlocking { arenaOrdsProxyClient.hentArbeidsgiver(1) } } returns ArbeidsgiverDTO(
            virksomhetsnummer = "111",
            organisasjonsnummerMorselskap = "456"
        )
        every { runBlocking { arenaOrdsProxyClient.hentArbeidsgiver(2) } } returns ArbeidsgiverDTO(
            virksomhetsnummer = "222",
            organisasjonsnummerMorselskap = "456"
        )

        every { runBlocking { amtEnhetsregister.hentVirksomhetsNavn(111) } } returns VirksomhetDTO(
            organisasjonsnummer = "789",
            navn = "Bedrift 1",
            overordnetEnhetOrganisasjonsnummer = "1011",
            overordnetEnhetNavn = "Overordnetbedrift 1"
        )
        every { runBlocking { amtEnhetsregister.hentVirksomhetsNavn(222) } } returns VirksomhetDTO(
            organisasjonsnummer = "7891",
            navn = "Bedrift 2",
            overordnetEnhetOrganisasjonsnummer = "1011",
            overordnetEnhetNavn = "Overordnetbedrift 2"
        )
    }

    test("henter navn på arrangør basert på virksomhetsnummer tilhørende arrangør id") {
        arrangorService.hentArrangorNavn(1) shouldBe "Overordnetbedrift 1"
        arrangorService.hentArrangorNavn(2) shouldBe "Overordnetbedrift 2"
    }

    test("arrangør navn blir cachet basert på arrangør id") {
        arrangorService.hentArrangorNavn(1)
        arrangorService.hentArrangorNavn(1)
        arrangorService.hentArrangorNavn(2)
        arrangorService.hentArrangorNavn(2)
        arrangorService.hentArrangorNavn(1)

        verify(exactly = 2) { runBlocking { amtEnhetsregister.hentVirksomhetsNavn(any()) } }
        verify(exactly = 2) { runBlocking { arenaOrdsProxyClient.hentArbeidsgiver(any()) } }
    }
})
