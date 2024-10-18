package no.nav.mulighetsrommet.api.services

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
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
import no.nav.mulighetsrommet.api.domain.dto.BrregVirksomhetDto
import no.nav.mulighetsrommet.api.repositories.ArrangorRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import java.time.LocalDate

class ArrangorServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    val underenhet = BrregVirksomhetDto(
        organisasjonsnummer = Organisasjonsnummer("234567891"),
        navn = "Underenhet til Testbedriften AS",
        overordnetEnhet = Organisasjonsnummer("123456789"),
        postnummer = null,
        poststed = null,
    )
    val hovedenhet = BrregVirksomhetDto(
        organisasjonsnummer = Organisasjonsnummer("123456789"),
        navn = "Testbedriften AS",
        underenheter = listOf(underenhet),
        postnummer = "0484",
        poststed = "Oslo",
    )

    context(ArrangorService::getOrSyncArrangorFromBrreg.name) {
        val brregClient: BrregClient = mockk()
        val arrangorRepository = ArrangorRepository(database.db)
        val arrangorService = ArrangorService(brregClient, arrangorRepository)

        afterEach {
            clearAllMocks()
            database.db.truncateAll()
        }

        test("skal synkronisere hovedenhet uten underenheter fra brreg til databasen gitt orgnr til hovedenhet") {
            coEvery { brregClient.getBrregVirksomhet(hovedenhet.organisasjonsnummer) } returns hovedenhet.right()

            arrangorService.getOrSyncArrangorFromBrreg(hovedenhet.organisasjonsnummer).shouldBeRight()

            arrangorRepository.get(hovedenhet.organisasjonsnummer).shouldNotBeNull().should {
                it.id.shouldNotBeNull()
                it.navn shouldBe "Testbedriften AS"
                it.organisasjonsnummer shouldBe Organisasjonsnummer("123456789")
                it.postnummer shouldBe "0484"
                it.poststed shouldBe "Oslo"
                it.underenheter.shouldBeNull()
            }
            arrangorRepository.get(underenhet.organisasjonsnummer).shouldBeNull()
        }

        test("skal synkronisere hovedenhet i tillegg til underenhet fra brreg til databasen gitt orgnr til underenhet") {
            coEvery { brregClient.getBrregVirksomhet(hovedenhet.organisasjonsnummer) } returns hovedenhet.right()
            coEvery { brregClient.getBrregVirksomhet(underenhet.organisasjonsnummer) } returns underenhet.right()

            arrangorService.getOrSyncArrangorFromBrreg(underenhet.organisasjonsnummer).shouldBeRight()

            arrangorRepository.get(hovedenhet.organisasjonsnummer).shouldNotBeNull().should {
                it.navn shouldBe "Testbedriften AS"
                it.organisasjonsnummer shouldBe Organisasjonsnummer("123456789")
            }
            arrangorRepository.get(underenhet.organisasjonsnummer).shouldNotBeNull().should {
                it.navn shouldBe "Underenhet til Testbedriften AS"
                it.organisasjonsnummer shouldBe Organisasjonsnummer("234567891")
            }
        }

        test("skal synkronisere slettet enhet fra brreg og til databasen gitt orgnr til enheten") {
            val orgnr = Organisasjonsnummer("100200300")
            val slettetVirksomhet = BrregVirksomhetDto(
                organisasjonsnummer = orgnr,
                navn = "Slettet bedrift",
                slettetDato = LocalDate.of(2020, 1, 1),
                postnummer = null,
                poststed = null,
            )

            coEvery { brregClient.getBrregVirksomhet(orgnr) } returns slettetVirksomhet.right()

            arrangorService.getOrSyncArrangorFromBrreg(orgnr).shouldBeRight()

            arrangorRepository.get(orgnr).shouldNotBeNull().should {
                it.navn shouldBe "Slettet bedrift"
                it.organisasjonsnummer shouldBe orgnr
                it.slettetDato shouldBe LocalDate.of(2020, 1, 1)
            }
        }

        test("NotFound error når enhet ikke finnes") {
            val orgnr = Organisasjonsnummer("123123123")

            coEvery { brregClient.getBrregVirksomhet(orgnr) } returns BrregError.NotFound.left()
            coEvery { brregClient.getBrregVirksomhet(orgnr) } returns BrregError.NotFound.left()

            arrangorService.getOrSyncArrangorFromBrreg(orgnr) shouldBeLeft BrregError.NotFound

            arrangorRepository.get(orgnr) shouldBe null
        }
    }
})
