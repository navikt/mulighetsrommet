package no.nav.mulighetsrommet.api.services

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.ktor.server.plugins.*
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.mulighetsrommet.api.clients.brreg.BrregClient
import no.nav.mulighetsrommet.api.domain.dto.VirksomhetDto
import no.nav.mulighetsrommet.api.repositories.VirksomhetRepository
import no.nav.mulighetsrommet.database.utils.query

class VirksomhetServiceTest : FunSpec({

    val brregClient: BrregClient = mockk()
    val virksomhetRepository: VirksomhetRepository = mockk()
    val virksomhetService = VirksomhetService(brregClient, virksomhetRepository)

    beforeSpec {
        every { virksomhetRepository.get(any()) } returns query { null }
        every { virksomhetRepository.upsert(any()) } returns query {}
        every { virksomhetRepository.upsertOverordnetEnhet(any()) } returns query {}

        coEvery { brregClient.hentEnhet("123456789") } returns
            VirksomhetDto(
                organisasjonsnummer = "123456789",
                navn = "Testbedriften AS",
                underenheter = listOf(
                    VirksomhetDto(
                        organisasjonsnummer = "234567891",
                        navn = "Underenhet til Testbedriften AS",
                        postnummer = null,
                        poststed = null,
                    ),
                ),
                postnummer = null,
                poststed = null,
            )

        coEvery { brregClient.hentEnhet("999999999") } throws NotFoundException("Fant ingen enhet i Brreg med orgnr: '999999999'")

        coEvery { brregClient.sokEtterOverordnetEnheter("ingen treff her") } returns emptyList()

        coEvery { brregClient.sokEtterOverordnetEnheter("Nav") } returns listOf(
            VirksomhetDto(
                organisasjonsnummer = "123456789",
                navn = "NAV AS",
                postnummer = null,
                poststed = null,
            ),
            VirksomhetDto(
                organisasjonsnummer = "445533227",
                navn = "Navesen AS",
                postnummer = null,
                poststed = null,
            ),
        )
    }

    test("Hent enhet skal hente enhet") {
        virksomhetService.getOrSyncVirksomhet("123456789")?.organisasjonsnummer shouldBe "123456789"
        virksomhetService.getOrSyncVirksomhet("123456789")?.navn shouldBe "Testbedriften AS"
        virksomhetService.getOrSyncVirksomhet("123456789")?.underenheter?.shouldContain(
            VirksomhetDto(
                organisasjonsnummer = "234567891",
                navn = "Underenhet til Testbedriften AS",
                postnummer = null,
                poststed = null,
            ),
        )
    }

    test("Hent enhet skal returnere 404 not found hvis ingen enhet finnes") {
        val exception = shouldThrow<NotFoundException> {
            virksomhetService.getOrSyncVirksomhet("999999999")
        }

        exception.message shouldBe "Fant ingen enhet i Brreg med orgnr: '999999999'"
    }

    test("Søk etter enhet skal returnere en liste med treff") {
        val sokestreng = "Nav"
        val result = virksomhetService.sokEtterEnhet(sokestreng)
        result.size shouldBe 2
        result[0].navn shouldBe "NAV AS"
        result[1].navn shouldBe "Navesen AS"
    }

    test("Søk etter enhet skal returnere en tom liste hvis ingen treff") {
        val sokestreng = "ingen treff her"
        val result = virksomhetService.sokEtterEnhet(sokestreng)
        result.size shouldBe 0
    }
})
