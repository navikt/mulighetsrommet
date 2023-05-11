package no.nav.mulighetsrommet.api.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.mulighetsrommet.api.clients.enhetsregister.AmtEnhetsregisterClient
import no.nav.mulighetsrommet.api.domain.dto.VirksomhetDto

class ArrangorServiceTest : FunSpec({

    val amtEnhetsregister: AmtEnhetsregisterClient = mockk()

    val arrangorService = ArrangorService(amtEnhetsregister)

    beforeSpec {
        coEvery { amtEnhetsregister.hentVirksomhet("111") } returns VirksomhetDto(
            organisasjonsnummer = "789",
            navn = "Bedrift 1",
            overordnetEnhet = "1011",
            underenheter = null,
        )
        coEvery { amtEnhetsregister.hentVirksomhet("222") } returns VirksomhetDto(
            organisasjonsnummer = "7891",
            navn = "Bedrift 2",
            overordnetEnhet = "1011",
            underenheter = null,
        )

        coEvery { amtEnhetsregister.hentVirksomhet("1011") } returns VirksomhetDto(
            organisasjonsnummer = "1011",
            navn = "Overordnetbedrift 1",
            overordnetEnhet = null,
            underenheter = null,
        )
    }

    test("henter navn på arrangør basert på virksomhetsnummer tilhørende arrangør id") {
        arrangorService.hentOverordnetEnhetNavnForArrangor("111") shouldBe "Overordnetbedrift 1"
        arrangorService.hentOverordnetEnhetNavnForArrangor("222") shouldBe "Overordnetbedrift 1"
    }

    test("arrangør navn blir cachet basert på arrangør id") {
        arrangorService.hentOverordnetEnhetNavnForArrangor("111")
        arrangorService.hentOverordnetEnhetNavnForArrangor("222")
        arrangorService.hentOverordnetEnhetNavnForArrangor("222")
        arrangorService.hentOverordnetEnhetNavnForArrangor("222")
        arrangorService.hentOverordnetEnhetNavnForArrangor("111")

        coVerify(exactly = 3) { amtEnhetsregister.hentVirksomhet(any()) }
    }
})
