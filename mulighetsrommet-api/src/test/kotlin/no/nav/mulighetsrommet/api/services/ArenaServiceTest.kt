package no.nav.mulighetsrommet.api.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.createDatabaseConfigWithRandomSchema
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseListener
import no.nav.mulighetsrommet.domain.adapter.AdapterSak
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltak
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltakdeltaker
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltaksgjennomforing
import no.nav.mulighetsrommet.domain.models.Deltakerstatus
import org.assertj.db.api.Assertions.assertThat
import org.assertj.db.type.Table
import java.time.LocalDateTime

class ArenaServiceTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val listener = FlywayDatabaseListener(createDatabaseConfigWithRandomSchema())

    register(listener)

    context("ArenaService") {
        val service = ArenaService(listener.db)

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
            val table = Table(listener.db.getDatasource(), "tiltakstype")

            service.upsertTiltakstype(tiltakstype)
            service.upsertTiltakstype(tiltakstype.copy(innsatsgruppe = 2))

            assertThat(table).row(0)
                .column("id").value().isEqualTo(1)
                .column("innsatsgruppe_id").value().isEqualTo(2)
        }

        test("upsert tiltaksgjennomføring") {
            val table = Table(listener.db.getDatasource(), "tiltaksgjennomforing")

            service.upsertTiltaksgjennomforing(tiltaksgjennomforing)
            service.upsertTiltaksgjennomforing(tiltaksgjennomforing.copy(navn = "Oppdatert arbeidstrening"))

            assertThat(table).row(0)
                .column("id").value().isEqualTo(1)
                .column("navn").value().isEqualTo("Oppdatert arbeidstrening")
        }

        test("upsert deltaker") {
            val table = Table(listener.db.getDatasource(), "deltaker")

            service.upsertDeltaker(deltaker)
            service.upsertDeltaker(deltaker.copy(status = Deltakerstatus.DELTAR))

            assertThat(table).row(0)
                .column("id").value().isEqualTo(1)
                .column("status").value().isEqualTo("DELTAR")
        }

        context("update tiltaksgjennomføring with sak") {
            test("should update tiltaksnummer when sak references tiltaksgjennomføring") {
                val table = Table(listener.db.getDatasource(), "tiltaksgjennomforing")

                service.updateTiltaksgjennomforingWithSak(sak)

                assertThat(table).row(0)
                    .column("id").value().isEqualTo(1)
                    .column("tiltaksnummer").value().isEqualTo(3)
            }

            test("should unset tiltaksnummer") {
                val table = Table(listener.db.getDatasource(), "tiltaksgjennomforing")

                service.unsetSakOnTiltaksgjennomforing(sak)

                assertThat(table).row(0)
                    .column("id").value().isEqualTo(1)
                    .column("tiltaksnummer").value().isNull
                    .column("aar").value().isNull
            }

            test("should not do an update when the sak does not reference any tiltaksgjennomføring") {
                service.updateTiltaksgjennomforingWithSak(sak.copy(id = 999)) shouldBe null
                service.unsetSakOnTiltaksgjennomforing(sak.copy(id = 999)) shouldBe null
            }
        }
    }
})
