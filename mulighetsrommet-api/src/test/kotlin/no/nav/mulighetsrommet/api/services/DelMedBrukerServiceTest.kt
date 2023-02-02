package no.nav.mulighetsrommet.api.services

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.string.shouldContain
import io.ktor.server.plugins.*
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.createApiDatabaseTestSchema
import no.nav.mulighetsrommet.domain.models.DelMedBruker
import org.assertj.db.api.Assertions.assertThat
import org.assertj.db.type.Table

class DelMedBrukerServiceTest : FunSpec({
    testOrder = TestCaseOrder.Sequential

    val database = extension(FlywayDatabaseTestListener(createApiDatabaseTestSchema()))

    context("DelMedBrukerService") {
        val service = DelMedBrukerService(database.db)
        val payload = DelMedBruker(
            norskIdent = "12345678910",
            navident = "nav123",
            sanityId = "123456",
            dialogId = "1234"
        )

        test("Insert del med bruker-data") {
            val table = Table(database.db.getDatasource(), "del_med_bruker")
            service.lagreDelMedBruker(payload)
            assertThat(table).row(0)
                .column("id").value().isEqualTo(1)
                .column("norsk_ident").value().isEqualTo("12345678910")
                .column("navident").value().isEqualTo("nav123")
                .column("sanity_id").value().isEqualTo("123456")
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
            val table = Table(database.db.getDatasource(), "del_med_bruker")
            service.getDeltMedBruker(
                fnr = "12345678910",
                sanityId = "123456"
            ).map {
                assertThat(table).row(0)
                    .column("id").value().isEqualTo(1)
                    .column("norsk_ident").value()
                    .isEqualTo(it?.norskIdent ?: "")
                    .column("navident").value().isEqualTo(it?.navident ?: "")
                    .column("sanity_id").value()
                    .isEqualTo(it?.sanityId ?: "")
            }
        }
    }
})
