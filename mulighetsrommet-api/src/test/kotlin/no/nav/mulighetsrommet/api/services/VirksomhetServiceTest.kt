package no.nav.mulighetsrommet.api.services

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.server.plugins.*
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.clients.brreg.BrregClient
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dto.VirksomhetDto
import no.nav.mulighetsrommet.api.repositories.VirksomhetRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener

class VirksomhetServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    val brregClient: BrregClient = mockk()
    lateinit var virksomhetRepository: VirksomhetRepository
    lateinit var virksomhetService: VirksomhetService

    beforeSpec {
        virksomhetRepository = VirksomhetRepository(database.db)
        virksomhetService = VirksomhetService(brregClient, virksomhetRepository)
    }

    context(VirksomhetService::sokEtterEnhet.name) {
        val testBedriftUnderenhet = VirksomhetDto(
            organisasjonsnummer = "234567891",
            navn = "Underenhet til Testbedriften AS",
            postnummer = null,
            poststed = null,
        )
        val testBedrift = VirksomhetDto(
            organisasjonsnummer = "123456789",
            navn = "Testbedriften AS",
            underenheter = listOf(testBedriftUnderenhet),
            postnummer = null,
            poststed = null,
        )

        test("skal hente enheter fra brreg og skrive de til databasen") {
            val hovedenhet = testBedrift.organisasjonsnummer

            coEvery { brregClient.hentEnhet(hovedenhet) } returns testBedrift

            virksomhetService.getOrSyncVirksomhet(hovedenhet) shouldBe testBedrift

            virksomhetRepository.get(hovedenhet) shouldBeRight testBedrift.copy(underenheter = null)
            virksomhetRepository.get(testBedriftUnderenhet.organisasjonsnummer) shouldBeRight testBedriftUnderenhet
        }

        test("noop når enhet ikke finnes i brreg") {
            val orgnr = "123123123"

            coEvery { brregClient.hentEnhet(orgnr) } returns null

            virksomhetService.getOrSyncVirksomhet(orgnr) shouldBe null

            virksomhetRepository.get(orgnr) shouldBeRight null
        }

        test("Hent enhet skal returnere 404 not found hvis ingen enhet finnes") {
            coEvery { brregClient.hentEnhet("999999999") } throws NotFoundException("Fant ingen enhet i Brreg med orgnr: '999999999'")

            val exception = shouldThrow<NotFoundException> {
                virksomhetService.getOrSyncVirksomhet("999999999")
            }

            exception.message shouldBe "Fant ingen enhet i Brreg med orgnr: '999999999'"
        }
    }

    context(VirksomhetService::sokEtterEnhet.name) {
        test("Søk etter enhet skal returnere en liste med treff") {
            val sok = "Nav"

            coEvery { brregClient.sokEtterOverordnetEnheter(sok) } returns listOf(
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

            val result = virksomhetService.sokEtterEnhet(sok)

            result.size shouldBe 2
            result[0].navn shouldBe "NAV AS"
            result[1].navn shouldBe "Navesen AS"
        }

        test("Søk etter enhet skal returnere en tom liste hvis ingen treff") {
            val sok = "ingen treff her"

            coEvery { brregClient.sokEtterOverordnetEnheter(sok) } returns emptyList()

            val result = virksomhetService.sokEtterEnhet(sok)

            result.size shouldBe 0
        }
    }
})
