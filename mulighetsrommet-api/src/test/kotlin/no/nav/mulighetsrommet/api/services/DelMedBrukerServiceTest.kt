package no.nav.mulighetsrommet.api.services

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.string.shouldContain
import io.ktor.server.plugins.*
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseListener
import no.nav.mulighetsrommet.database.kotest.extensions.createApiDatabaseTestSchema
import no.nav.mulighetsrommet.domain.models.DelMedBruker
import org.assertj.db.api.Assertions.assertThat
import org.assertj.db.type.Table

class DelMedBrukerServiceTest : FunSpec({
    testOrder = TestCaseOrder.Sequential

    val listener = FlywayDatabaseListener(createApiDatabaseTestSchema())

    register(listener)

    context("DelMedBrukerService") {
        val service = DelMedBrukerService(listener.db)
        val payload = DelMedBruker(
            bruker_fnr = "12345678910",
            navident = "nav123",
            tiltaksnummer = "123456"
        )

        test("Insert del med bruker-data") {
            val table = Table(listener.db.getDatasource(), "del_med_bruker")
            service.lagreDelMedBruker(payload)
            assertThat(table).row(0)
                .column("id").value().isEqualTo(1)
                .column("bruker_fnr").value().isEqualTo("12345678910")
                .column("navident").value().isEqualTo("nav123")
                .column("tiltaksnummer").value().isEqualTo("123456")
        }

        test("Lagre til tabell feiler dersom input for veileders navident er feil") {
            val payloadMedFeilData = payload.copy(
                navident = "nav12345"
            )
            val exception = shouldThrow<BadRequestException> {
                service.lagreDelMedBruker(payloadMedFeilData)
            }

            exception.message shouldContain "Veileders NAVident er ikke 6 tegn"
        }

        test("Lagre til tabell feiler dersom input for brukers fnr er ulikt 11 tegn") {
            val payloadMedFeilData = payload.copy(
                bruker_fnr = "12345678910123"
            )
            val exception = shouldThrow<BadRequestException> {
                service.lagreDelMedBruker(payloadMedFeilData)
            }

            exception.message shouldContain "Brukers fnr er ikke 11 tegn"
        }

        test("Les fra tabell") {
            val table = Table(listener.db.getDatasource(), "del_med_bruker")
            service.getDeltMedBruker(
                fnr = "12345678910",
                navident = "nav123",
                tiltaksnummer = "123456"
            ).map {
                assertThat(table).row(0)
                    .column("id").value().isEqualTo(1)
                    .column("bruker_fnr").value().isEqualTo(it?.bruker_fnr ?: "")
                    .column("navident").value().isEqualTo(it?.navident ?: "")
                    .column("tiltaksnummer").value().isEqualTo(it?.tiltaksnummer ?: "")
            }
        }
    }
})
