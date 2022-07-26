package no.nav.mulighetsrommet.api.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.collections.shouldHaveSize
import no.nav.mulighetsrommet.api.createDatabaseConfigWithRandomSchema
import no.nav.mulighetsrommet.database.kotest.extensions.DatabaseListener
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltak
import java.time.LocalDateTime

class TiltakstypeServiceTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val listener = DatabaseListener(createDatabaseConfigWithRandomSchema())

    register(listener)

    beforeSpec {
        val arenaService = ArenaService(listener.db)

        val tiltakstype = AdapterTiltak(
            navn = "Arbeidstrening",
            innsatsgruppe = 1,
            tiltakskode = "ARBTREN",
            fraDato = LocalDateTime.now(),
            tilDato = LocalDateTime.now().plusYears(1)
        )

        val tiltakstype2 = AdapterTiltak(
            navn = "Oppfølging",
            innsatsgruppe = 2,
            tiltakskode = "INDOPPFOLG",
            fraDato = LocalDateTime.now(),
            tilDato = LocalDateTime.now().plusYears(1)
        )

        arenaService.upsertTiltakstype(tiltakstype)
        arenaService.upsertTiltakstype(tiltakstype2)
    }

    context("CRUD") {
        val service = TiltakstypeService(listener.db)

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
    }
})
