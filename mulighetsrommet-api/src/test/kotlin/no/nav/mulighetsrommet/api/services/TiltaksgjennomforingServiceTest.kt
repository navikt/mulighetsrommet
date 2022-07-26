package no.nav.mulighetsrommet.api.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.createDatabaseConfigWithRandomSchema
import no.nav.mulighetsrommet.database.kotest.extensions.DatabaseListener
import no.nav.mulighetsrommet.domain.Tiltaksgjennomforing
import no.nav.mulighetsrommet.domain.adapter.AdapterSak
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltak
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltaksgjennomforing
import java.time.LocalDateTime

class TiltaksgjennomforingServiceTest : FunSpec({

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

        val tiltaksgjennomforing = AdapterTiltaksgjennomforing(
            navn = "Oppfølging",
            arrangorId = 1,
            tiltakskode = "INDOPPFOLG",
            id = 1,
            sakId = 1,
        )

        val tiltaksgjennomforing2 = AdapterTiltaksgjennomforing(
            navn = "Trening",
            arrangorId = 1,
            tiltakskode = "ARBTREN",
            id = 2,
            sakId = 2,
        )

        arenaService.upsertTiltakstype(tiltakstype)
        arenaService.upsertTiltakstype(tiltakstype2)
        arenaService.upsertTiltaksgjennomforing(tiltaksgjennomforing)
        arenaService.upsertTiltaksgjennomforing(tiltaksgjennomforing2)
        arenaService.updateTiltaksgjennomforingWithSak(
            AdapterSak(id = 1, lopenummer = 11, aar = 2022)
        )
        arenaService.updateTiltaksgjennomforingWithSak(
            AdapterSak(id = 2, lopenummer = 22, aar = 2022)
        )
    }

    context("CRUD") {
        val service = TiltaksgjennomforingService(listener.db)

        test("should get tiltaksgjennomføring by id") {
            val tiltaksgjennomforing = service.getTiltaksgjennomforingById(1)

            tiltaksgjennomforing shouldBe Tiltaksgjennomforing(
                id = 1,
                navn = "Oppfølging",
                tiltakskode = "INDOPPFOLG",
                tiltaksnummer = 11,
                aar = 2022
            )
        }

        test("should get tiltaksgjennomføringer") {
            service.getTiltaksgjennomforinger() shouldHaveSize 2
        }

        test("should get tiltaksgjennomføringer by tiltakskode") {
            service.getTiltaksgjennomforingerByTiltakskode("ARBTREN") shouldHaveSize 1
        }
    }
})
