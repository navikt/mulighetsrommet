package no.nav.mulighetsrommet.api.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.createDatabaseConfigWithRandomSchema
import no.nav.mulighetsrommet.domain.Tiltakstype
import no.nav.mulighetsrommet.test.extensions.DatabaseListener
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class TiltakstypeServiceTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val listener = DatabaseListener(createDatabaseConfigWithRandomSchema())

    register(listener)

    context("CRUD") {
        val service = TiltakstypeService(listener.db, LoggerFactory.getLogger("TiltakstypeService"))

        test("should create tiltakstype") {
            val tiltakstype0 = service.createTiltakstype(
                Tiltakstype(
                    navn = "Arbeidstrening",
                    innsatsgruppe = 1,
                    tiltakskode = "ARBTREN",
                    fraDato = LocalDateTime.now(),
                    tilDato = LocalDateTime.now().plusYears(1)
                )
            )

            val tiltakstype1 = service.createTiltakstype(
                Tiltakstype(
                    navn = "Oppfølging",
                    innsatsgruppe = 2,
                    tiltakskode = "INDOPPFOLG",
                    fraDato = LocalDateTime.now(),
                    tilDato = LocalDateTime.now().plusYears(1)
                )
            )

            tiltakstype0.id shouldBe 1
            tiltakstype1.id shouldBe 2
        }

        test("should read tiltakstyper") {
            service.getTiltakstyper() shouldHaveSize 2
        }

        test("should filter tiltakstyper by innsatsgruppe") {
            service.getTiltakstyper(innsatsgrupper = listOf(1)) shouldHaveSize 1
            service.getTiltakstyper(innsatsgrupper = listOf(1, 2)) shouldHaveSize 2
            service.getTiltakstyper(innsatsgrupper = listOf(3)) shouldHaveSize 0
        }

        test("should filter tiltakstyper by search") {
            service.getTiltakstyper(search = "Førerhund") shouldHaveSize 0
            service.getTiltakstyper(search = "Arbeidstrening") shouldHaveSize 1
        }

        test("should update tiltakstype") {
            val tiltakstype = service.updateTiltakstype(
                "ARBTREN",
                Tiltakstype(
                    navn = "Abist",
                    innsatsgruppe = 1,
                    tiltakskode = "ABIST"
                )
            )

            tiltakstype shouldBe Tiltakstype(
                id = 1,
                navn = "Abist",
                innsatsgruppe = 1,
                tiltakskode = "ABIST"
            )
        }
    }
})
