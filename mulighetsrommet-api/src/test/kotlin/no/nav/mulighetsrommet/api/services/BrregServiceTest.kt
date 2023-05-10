package no.nav.mulighetsrommet.api.services

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.ktor.server.plugins.*
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.clients.brreg.BrregClient
import no.nav.mulighetsrommet.api.clients.brreg.BrregEnhetDto
import no.nav.mulighetsrommet.api.clients.brreg.BrregEnhetUtenUnderenheterDto

class BrregServiceTest : FunSpec({

    val brregClient: BrregClient = mockk()
    val brregService = BrregService(brregClient)

    beforeSpec {
        coEvery { brregClient.hentEnhet("123456789") } returns BrregEnhetDto(
            organisasjonsnummer = "123456789",
            navn = "Testbedriften AS",
            underenheter = listOf(
                BrregEnhetDto(
                    organisasjonsnummer = "234567891",
                    navn = "Underenhet til Testbedriften AS",
                ),
            ),
        )

        coEvery { brregClient.hentEnhet("999999999") } throws NotFoundException("Fant ingen enhet i Brreg med orgnr: '999999999'")

        coEvery { brregClient.sokEtterOverordnetEnheter("ingen treff her") } returns emptyList()

        coEvery { brregClient.sokEtterOverordnetEnheter("Nav") } returns listOf(
            BrregEnhetUtenUnderenheterDto(
                organisasjonsnummer = "123456789",
                navn = "NAV AS",
            ),
            BrregEnhetUtenUnderenheterDto(
                organisasjonsnummer = "445533227",
                navn = "Navesen AS",
            ),
        )
    }

    test("Hent enhet skal hente enhet") {
        brregService.hentEnhet("123456789").organisasjonsnummer shouldBe "123456789"
        brregService.hentEnhet("123456789").navn shouldBe "Testbedriften AS"
        brregService.hentEnhet("123456789").underenheter?.shouldContain(
            BrregEnhetDto(
                organisasjonsnummer = "234567891",
                navn = "Underenhet til Testbedriften AS",
            ),
        )
    }

    test("Hent enhet skal returnere 404 not found hvis ingen enhet finnes") {
        val exception = shouldThrow<NotFoundException> {
            brregService.hentEnhet("999999999")
        }

        exception.message shouldBe "Fant ingen enhet i Brreg med orgnr: '999999999'"
    }

    test("Søk etter enhet skal returnere en liste med treff") {
        val sokestreng = "Nav"
        val result = brregService.sokEtterEnhet(sokestreng)
        result.size shouldBe 2
        result[0].navn shouldBe "NAV AS"
        result[1].navn shouldBe "Navesen AS"
    }

    test("Søk etter enhet skal returnere en tom liste hvis ingen treff") {
        val sokestreng = "ingen treff her"
        val result = brregService.sokEtterEnhet(sokestreng)
        result.size shouldBe 0
    }
})
