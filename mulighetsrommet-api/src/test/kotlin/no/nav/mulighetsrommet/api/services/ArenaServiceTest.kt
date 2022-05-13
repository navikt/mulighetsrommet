package no.nav.mulighetsrommet.api.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.createDatabaseConfigWithRandomSchema
import no.nav.mulighetsrommet.domain.Deltakerstatus
import no.nav.mulighetsrommet.domain.adapter.AdapterSak
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltak
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltakdeltaker
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltaksgjennomforing
import no.nav.mulighetsrommet.test.extensions.DatabaseListener
import org.assertj.db.api.Assertions.assertThat
import org.assertj.db.type.Table
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class ArenaServiceTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val listener = DatabaseListener(createDatabaseConfigWithRandomSchema())

    register(listener)

    context("ArenaService") {

        val service = ArenaService(listener.db, LoggerFactory.getLogger("ArenaService"))

        val tiltakstype = AdapterTiltak(
            navn = "Arbeidstrening",
            innsatsgruppe = 1,
            tiltakskode = "ARBTREN",
            fraDato = LocalDateTime.now(),
            tilDato = LocalDateTime.now().plusYears(1)
        )

        val tiltaksgjennomforing = AdapterTiltaksgjennomforing(
            navn = "Arbeidstrening",
            arrangorId = 1,
            tiltakskode = "ARBTREN",
            id = 123,
            sakId = 123,
        )

        val deltaker = AdapterTiltakdeltaker(
            id = 123,
            tiltaksgjennomforingId = 123,
            personId = 111,
            status = Deltakerstatus.VENTER
        )

        val sak = AdapterSak(
            id = 123,
            lopenummer = 3,
            aar = 2022,
        )

        test("upsert tiltakstype") {
            val table = Table(listener.db.dataSource, "tiltakstype")

            service.upsertTiltakstype(tiltakstype)
            service.upsertTiltakstype(tiltakstype.copy(innsatsgruppe = 2))

            assertThat(table).row(0)
                .column("id").value().isEqualTo(1)
                .column("innsatsgruppe_id").value().isEqualTo(2)
        }

        test("upsert tiltaksgjennomføring") {
            val table = Table(listener.db.dataSource, "tiltaksgjennomforing")

            service.upsertTiltaksgjennomforing(tiltaksgjennomforing)
            service.upsertTiltaksgjennomforing(tiltaksgjennomforing.copy(navn = "Oppdatert arbeidstrening"))

            assertThat(table).row(0)
                .column("id").value().isEqualTo(1)
                .column("navn").value().isEqualTo("Oppdatert arbeidstrening")
        }

        test("upsert deltaker") {
            val table = Table(listener.db.dataSource, "deltaker")

            service.upsertDeltaker(deltaker)
            service.upsertDeltaker(deltaker.copy(status = Deltakerstatus.DELTAR))

            assertThat(table).row(0)
                .column("id").value().isEqualTo(1)
                .column("status").value().isEqualTo("DELTAR")
        }

        context("update tiltaksgjennomføring with sak") {
            val table = Table(listener.db.dataSource, "tiltaksgjennomforing")
            test("should update tiltaksnummer when sak references tiltaksgjennomføring") {
                service.updateTiltaksgjennomforingWithSak(sak)

                assertThat(table).row(0)
                    .column("id").value().isEqualTo(1)
                    .column("tiltaksnummer").value().isEqualTo(3)
            }

            test("should not do an update when the sak does not reference any tiltaksgjennomføring") {
                val notUpdated = service.updateTiltaksgjennomforingWithSak(sak.copy(id = 999))
                notUpdated shouldBe null
            }
        }
    }
})
