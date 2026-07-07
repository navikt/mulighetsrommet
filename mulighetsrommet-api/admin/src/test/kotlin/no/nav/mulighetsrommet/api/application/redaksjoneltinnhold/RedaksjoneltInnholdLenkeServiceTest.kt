package no.nav.mulighetsrommet.api.application.redaksjoneltinnhold

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import no.nav.mulighetsrommet.api.application.testing.TestAdminDatabase
import java.util.UUID

class RedaksjoneltInnholdLenkeServiceTest : FunSpec({
    val db = TestAdminDatabase()
    val service = RedaksjoneltInnholdLenkeService(db)

    context("delete") {
        test("returnerer Right når lenken ikke er i bruk") {
            every { db.queries.tiltakstype.getNamesReferencingLenke(any()) } returns emptyList()

            service.delete(UUID.randomUUID()).shouldBeRight()
        }

        test("returnerer Left med tiltakstypenavnene når lenken er i bruk") {
            val names = listOf("Tiltakstype A", "Tiltakstype B")
            every { db.queries.tiltakstype.getNamesReferencingLenke(any()) } returns names

            service.delete(UUID.randomUUID()) shouldBeLeft names
        }
    }
})
