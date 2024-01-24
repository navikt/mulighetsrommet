package no.nav.mulighetsrommet.api.services

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.clients.brreg.BrregClient
import no.nav.mulighetsrommet.api.clients.brreg.BrregError
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dto.VirksomhetDto
import no.nav.mulighetsrommet.api.repositories.VirksomhetRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import java.time.LocalDate

class VirksomhetServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    val brregClient: BrregClient = mockk()
    lateinit var virksomhetRepository: VirksomhetRepository
    lateinit var virksomhetService: VirksomhetService

    beforeSpec {
        virksomhetRepository = VirksomhetRepository(database.db)
        virksomhetService = VirksomhetService(brregClient, virksomhetRepository)
    }

    context(VirksomhetService::getOrSyncHovedenhetFromBrreg.name) {
        val underenhet = VirksomhetDto(
            organisasjonsnummer = "234567891",
            navn = "Underenhet til Testbedriften AS",
            overordnetEnhet = "123456789",
            postnummer = null,
            poststed = null,
        )
        val hovedenhet = VirksomhetDto(
            organisasjonsnummer = "123456789",
            navn = "Testbedriften AS",
            underenheter = listOf(underenhet),
            postnummer = "0484",
            poststed = "Oslo",
        )

        afterEach {
            clearAllMocks()
            database.db.truncateAll()
        }

        test("skal synkronisere hovedenhet med underenheter fra brreg til databasen gitt orgnr til hovedenhet") {
            coEvery { brregClient.getHovedenhet(hovedenhet.organisasjonsnummer) } returns hovedenhet.right()

            virksomhetService.getOrSyncHovedenhetFromBrreg(hovedenhet.organisasjonsnummer).shouldBeRight()

            virksomhetRepository.get(hovedenhet.organisasjonsnummer).shouldNotBeNull().should {
                it.id.shouldNotBeNull()
                it.navn shouldBe "Testbedriften AS"
                it.organisasjonsnummer shouldBe "123456789"
                it.postnummer shouldBe "0484"
                it.poststed shouldBe "Oslo"
                it.underenheter.shouldNotBeNull().shouldHaveSize(1).first().should { e ->
                    e.navn shouldBe "Underenhet til Testbedriften AS"
                    e.organisasjonsnummer shouldBe "234567891"
                }
            }
            virksomhetRepository.get(underenhet.organisasjonsnummer).shouldNotBeNull().should {
                it.id.shouldNotBeNull()
                it.navn shouldBe "Underenhet til Testbedriften AS"
                it.organisasjonsnummer shouldBe "234567891"
                it.underenheter.shouldBeNull()
            }
        }

        test("skal synkronisere hovedenhet med underenheter fra brreg til databasen gitt orgnr til underenhet") {
            coEvery { brregClient.getHovedenhet(hovedenhet.organisasjonsnummer) } returns hovedenhet.right()
            coEvery { brregClient.getHovedenhet(underenhet.organisasjonsnummer) } returns BrregError.NotFound.left()
            coEvery { brregClient.getUnderenhet(underenhet.organisasjonsnummer) } returns underenhet.right()

            virksomhetService.getOrSyncHovedenhetFromBrreg(underenhet.organisasjonsnummer).shouldBeRight()

            virksomhetRepository.get(hovedenhet.organisasjonsnummer).shouldNotBeNull().should {
                it.navn shouldBe "Testbedriften AS"
                it.organisasjonsnummer shouldBe "123456789"
            }
            virksomhetRepository.get(underenhet.organisasjonsnummer).shouldNotBeNull().should {
                it.navn shouldBe "Underenhet til Testbedriften AS"
                it.organisasjonsnummer shouldBe "234567891"
            }
        }

        test("skal synkronisere slettet enhet fra brreg og til databasen gitt orgnr til enheten") {
            val orgnr = "100200300"
            val slettetVirksomhet = VirksomhetDto(
                organisasjonsnummer = orgnr,
                navn = "Slettet bedrift",
                slettetDato = LocalDate.of(2020, 1, 1),
                postnummer = null,
                poststed = null,
            )

            coEvery { brregClient.getHovedenhet(orgnr) } returns slettetVirksomhet.right()

            virksomhetService.getOrSyncHovedenhetFromBrreg(orgnr).shouldBeRight()

            virksomhetRepository.get(orgnr).shouldNotBeNull().should {
                it.navn shouldBe "Slettet bedrift"
                it.organisasjonsnummer shouldBe orgnr
                it.slettetDato shouldBe LocalDate.of(2020, 1, 1)
            }
        }

        test("NotFound error når enhet ikke finnes") {
            val orgnr = "123123123"

            coEvery { brregClient.getHovedenhet(orgnr) } returns BrregError.NotFound.left()
            coEvery { brregClient.getUnderenhet(orgnr) } returns BrregError.NotFound.left()

            virksomhetService.getOrSyncHovedenhetFromBrreg(orgnr) shouldBeLeft BrregError.NotFound

            virksomhetRepository.get(orgnr) shouldBe null
        }
    }
})
