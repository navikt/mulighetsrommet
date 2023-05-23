package no.nav.mulighetsrommet.api.repositories

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dto.VirksomhetDto
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import java.util.*

class VirksomhetRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    beforeEach {
        database.db.clean()
        database.db.migrate()
    }

    context("crud") {
        test("Upsert virksomhet med underenheter") {
            val virksomhetRepository = VirksomhetRepository(database.db)

            val underenhet1 = VirksomhetDto(
                organisasjonsnummer = "880907522",
                overordnetEnhet = "982254604",
                navn = "REMA 1000 NORGE AS REGION NORDLAND",
            )
            val underenhet2 = VirksomhetDto(
                organisasjonsnummer = "912704327",
                overordnetEnhet = "982254604",
                navn = "REMA 1000 NORGE AS REGION VESTRE ØSTLAND",
            )
            val underenhet3 = VirksomhetDto(
                organisasjonsnummer = "912704394",
                overordnetEnhet = "982254604",
                navn = "REMA 1000 NORGE AS REGION NORD",
            )

            val virksomhet = VirksomhetDto(
                navn = "REMA 1000 AS",
                organisasjonsnummer = "982254604",
                underenheter = listOf(underenhet1, underenhet2, underenhet3),
            )
            virksomhetRepository.upsert(virksomhet).shouldBeRight()

            virksomhetRepository.get(virksomhet.organisasjonsnummer).shouldBeRight().should {
                it!!.navn shouldBe "REMA 1000 AS"
                it.underenheter!! shouldHaveSize 3
                it.underenheter!! shouldContainAll listOf(underenhet1, underenhet2, underenhet3)
            }

            virksomhetRepository.upsert(virksomhet.copy(underenheter = listOf(underenhet1))).shouldBeRight()
            virksomhetRepository.get(virksomhet.organisasjonsnummer).shouldBeRight().should {
                it!!.underenheter!! shouldHaveSize 1
                it.underenheter!! shouldContain underenhet1
            }
        }

        test("Upsert underenhet") {
            val virksomhetRepository = VirksomhetRepository(database.db)

            val underenhet1 = VirksomhetDto(
                organisasjonsnummer = "880907522",
                overordnetEnhet = "982254604",
                navn = "REMA 1000 NORGE AS REGION NORDLAND",
            )

            val underenhet2 = VirksomhetDto(
                organisasjonsnummer = "912704327",
                overordnetEnhet = "982254604",
                navn = "REMA 1000 NORGE AS REGION VESTRE ØSTLAND",
            )
            val underenhet3 = VirksomhetDto(
                organisasjonsnummer = "912704394",
                overordnetEnhet = "982254604",
                navn = "REMA 1000 NORGE AS REGION NORD",
            )

            val virksomhet = VirksomhetDto(
                navn = "REMA 1000 AS",
                organisasjonsnummer = "982254604",
                underenheter = listOf(underenhet1, underenhet2, underenhet3),
            )
            virksomhetRepository.upsert(virksomhet).shouldBeRight()

            virksomhetRepository.get(virksomhet.organisasjonsnummer).shouldBeRight().should {
                it!!.navn shouldBe "REMA 1000 AS"
                it.underenheter!! shouldHaveSize 3
                it.underenheter!! shouldContainAll listOf(underenhet1, underenhet2, underenhet3)
            }

            virksomhetRepository.upsert(virksomhet.copy(underenheter = listOf(underenhet1))).shouldBeRight()
            virksomhetRepository.get(virksomhet.organisasjonsnummer).shouldBeRight().should {
                it!!.underenheter!! shouldHaveSize 1
                it.underenheter!! shouldContain underenhet1
            }
        }

        test("Upsert underenhet etter overenhet") {
            val virksomhetRepository = VirksomhetRepository(database.db)

            val underenhet1 = VirksomhetDto(
                organisasjonsnummer = "880907522",
                overordnetEnhet = "982254604",
                navn = "REMA 1000 NORGE AS REGION NORDLAND",
            )

            val virksomhet = VirksomhetDto(
                navn = "REMA 1000 AS",
                organisasjonsnummer = "982254604",
                underenheter = listOf(), // Tom først
            )

            virksomhetRepository.upsert(virksomhet).shouldBeRight()
            virksomhetRepository.upsert(underenhet1).shouldBeRight()

            virksomhetRepository.get(underenhet1.organisasjonsnummer).shouldBeRight().should {
                it!!.organisasjonsnummer shouldBe underenhet1.organisasjonsnummer
            }
            virksomhetRepository.get(virksomhet.organisasjonsnummer).shouldBeRight().should {
                it!!.underenheter shouldContainExactly listOf(underenhet1)
            }
        }

        test("Delete overordnet cascader") {
            val virksomhetRepository = VirksomhetRepository(database.db)

            val underenhet1 = VirksomhetDto(
                organisasjonsnummer = "880907522",
                overordnetEnhet = "982254604",
                navn = "REMA 1000 NORGE AS REGION NORDLAND",
            )

            val virksomhet = VirksomhetDto(
                navn = "REMA 1000 AS",
                organisasjonsnummer = "982254604",
                underenheter = listOf(underenhet1),
            )
            virksomhetRepository.upsert(virksomhet).shouldBeRight()

            virksomhetRepository.get(underenhet1.organisasjonsnummer).shouldBeRight().should {
                it!!.organisasjonsnummer shouldBe underenhet1.organisasjonsnummer
            }

            virksomhetRepository.delete(virksomhet.organisasjonsnummer).shouldBeRight()
            virksomhetRepository.get(underenhet1.organisasjonsnummer).shouldBeRight().should {
                it shouldBe null
            }
            virksomhetRepository.get(virksomhet.organisasjonsnummer).shouldBeRight().should {
                it shouldBe null
            }
        }
    }
})
