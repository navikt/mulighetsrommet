package no.nav.mulighetsrommet.api.services

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import no.nav.mulighetsrommet.api.repositories.ArenaRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseListener
import no.nav.mulighetsrommet.database.kotest.extensions.createApiDatabaseTestSchema
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

    val listener = FlywayDatabaseListener(createApiDatabaseTestSchema())

    register(listener)

    context("ArenaService") {
        val repository = ArenaRepository(listener.db)
        val service = ArenaService(repository)

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
            tiltaksgjennomforingId = 123,
            sakId = 123,
        )

        val deltaker = AdapterTiltakdeltaker(
            tiltaksdeltakerId = 123,
            tiltaksgjennomforingId = 123,
            personId = 111,
            status = Deltakerstatus.VENTER
        )

        val sak = AdapterSak(
            sakId = 123,
            lopenummer = 3,
            aar = 2022
        )

        test("upsert tiltakstype") {
            val table = Table(listener.db.getDatasource(), "tiltakstype")

            service.createOrUpdate(tiltakstype)
            service.createOrUpdate(tiltakstype.copy(innsatsgruppe = 2))

            assertThat(table).row(0)
                .column("id").value().isEqualTo(1)
                .column("innsatsgruppe_id").value().isEqualTo(2)
        }

        test("upsert tiltaksgjennomføring") {
            val table = Table(listener.db.getDatasource(), "tiltaksgjennomforing")

            service.createOrUpdate(tiltaksgjennomforing)
            service.createOrUpdate(tiltaksgjennomforing.copy(navn = "Oppdatert arbeidstrening"))

            assertThat(table).row(0)
                .column("id").value().isEqualTo(1)
                .column("navn").value().isEqualTo("Oppdatert arbeidstrening")
        }

        test("upsert deltaker") {
            val table = Table(listener.db.getDatasource(), "deltaker")

            service.createOrUpdate(deltaker)
            service.createOrUpdate(deltaker.copy(status = Deltakerstatus.DELTAR))

            assertThat(table).row(0)
                .column("id").value().isEqualTo(1)
                .column("status").value().isEqualTo("DELTAR")
        }

        context("update tiltaksgjennomføring with sak") {
            test("should update tiltaksnummer when sak references tiltaksgjennomføring") {
                val table = Table(listener.db.getDatasource(), "tiltaksgjennomforing")

                service.setTiltaksnummerWith(sak)

                assertThat(table).row(0)
                    .column("id").value().isEqualTo(1)
                    .column("tiltaksnummer").value().isEqualTo(3)
            }

            test("should unset tiltaksnummer") {
                val table = Table(listener.db.getDatasource(), "tiltaksgjennomforing")

                service.removeTiltaksnummerWith(sak)

                assertThat(table).row(0)
                    .column("id").value().isEqualTo(1)
                    .column("tiltaksnummer").value().isNull
                    .column("aar").value().isNull
            }

            test("should not do an update when the sak does not reference any tiltaksgjennomføring") {
                service.setTiltaksnummerWith(sak.copy(sakId = 999)) shouldBeRight null
                service.removeTiltaksnummerWith(sak.copy(sakId = 999)) shouldBeRight null
            }
        }
    }
})
