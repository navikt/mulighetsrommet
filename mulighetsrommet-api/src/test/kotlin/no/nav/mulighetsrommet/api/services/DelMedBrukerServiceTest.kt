package no.nav.mulighetsrommet.api.services

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.server.plugins.*
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.DelMedBrukerDbo
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener

class DelMedBrukerServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    beforeEach {
        database.db.migrate()
    }

    afterEach {
        database.db.clean()
    }

    context("DelMedBrukerService") {
        val service = DelMedBrukerService(database.db)

        val payload = DelMedBrukerDbo(
            norskIdent = "12345678910",
            navident = "nav123",
            sanityId = "123456",
            dialogId = "1234"
        )

        test("Insert del med bruker-data") {
            service.lagreDelMedBruker(payload)

            database.assertThat("del_med_bruker").row(0)
                .value("id").isEqualTo(1)
                .value("norsk_ident").isEqualTo("12345678910")
                .value("navident").isEqualTo("nav123")
                .value("sanity_id").isEqualTo("123456")
        }

        test("Lagre til tabell feiler dersom input for brukers fnr er ulikt 11 tegn") {
            val payloadMedFeilData = payload.copy(
                norskIdent = "12345678910123"
            )
            val exception = shouldThrow<BadRequestException> {
                service.lagreDelMedBruker(payloadMedFeilData)
            }

            exception.message shouldContain "Brukers fnr er ikke 11 tegn"
        }

        test("Les fra tabell") {
            service.lagreDelMedBruker(payload)

            val delMedBruker = service.getDeltMedBruker(
                fnr = "12345678910",
                sanityId = "123456"
            )

            delMedBruker.shouldBeRight().should {
                it.shouldNotBeNull()

                it.id shouldBe "1"
                it.norskIdent shouldBe "12345678910"
                it.navident shouldBe "nav123"
                it.sanityId shouldBe "123456"
                it.dialogId shouldBe "1234"
            }
        }
    }
})
