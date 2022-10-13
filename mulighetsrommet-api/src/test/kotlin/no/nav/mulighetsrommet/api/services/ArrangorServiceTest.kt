package no.nav.mulighetsrommet.api.services

import io.kotest.common.runBlocking
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.mulighetsrommet.api.clients.amtenhetsregister.AmtEnhetsregisterClient
import no.nav.mulighetsrommet.api.clients.arenaordsproxy.ArenaOrdsProxyClient
import no.nav.mulighetsrommet.api.domain.ArbeidsgiverDTO
import no.nav.mulighetsrommet.api.domain.VirksomhetDTO

class ArrangorServiceTest : FunSpec({

    val arenaOrdsProxyClient: ArenaOrdsProxyClient = mockk()
    val amtEnhetsregister: AmtEnhetsregisterClient = mockk()

    val arrangorService =
        ArrangorService(arenaOrdsProxyClient, amtEnhetsregister)

    test("henter navn på virksomhet basert på virksomhetsnummer") {
        every { runBlocking { arenaOrdsProxyClient.hentArbeidsgiver(1) } } returns ArbeidsgiverDTO(
            virksomhetsnummer = "111",
            organisasjonsnummerMorselskap = "456"
        )
        every { runBlocking { arenaOrdsProxyClient.hentArbeidsgiver(2) } } returns ArbeidsgiverDTO(
            virksomhetsnummer = "222",
            organisasjonsnummerMorselskap = "456"
        )

        every { runBlocking { amtEnhetsregister.hentVirksomhetsNavn(any()) } } returns VirksomhetDTO(
            organisasjonsnummer = "7891",
            navn = "Bedrift 2",
            overordnetEnhetOrganisasjonsnummer = "1011",
            overordnetEnhetNavn = "Overordnetbedrift 2"
        )
        every { runBlocking { amtEnhetsregister.hentVirksomhetsNavn(111) } } returns VirksomhetDTO(
            organisasjonsnummer = "789",
            navn = "Bedrift 1",
            overordnetEnhetOrganisasjonsnummer = "1011",
            overordnetEnhetNavn = "Overordnetbedrift 1"
        )

        arrangorService.hentArrangorNavn(1) shouldBe "Bedrift 1"
        arrangorService.hentArrangorNavn(2) shouldBe "Bedrift 2"
    }
})
