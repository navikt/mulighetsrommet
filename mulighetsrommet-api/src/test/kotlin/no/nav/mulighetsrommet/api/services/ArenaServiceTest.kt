package no.nav.mulighetsrommet.api.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import no.nav.mulighetsrommet.api.repositories.DeltakerRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.createApiDatabaseTestSchema
import no.nav.mulighetsrommet.domain.models.Deltaker
import no.nav.mulighetsrommet.domain.models.Deltakerstatus
import no.nav.mulighetsrommet.domain.models.Tiltaksgjennomforing
import no.nav.mulighetsrommet.domain.models.Tiltakstype
import org.assertj.db.api.Assertions.assertThat
import org.assertj.db.type.Table
import java.time.LocalDateTime
import java.util.*

class ArenaServiceTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val database = extension(FlywayDatabaseTestListener(createApiDatabaseTestSchema()))

    context("ArenaService") {

        val tiltakstypeRepository = TiltakstypeRepository(database.db)
        val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)
        val deltakerRepository = DeltakerRepository(database.db)
        val service = ArenaService(tiltakstypeRepository, tiltaksgjennomforingRepository, deltakerRepository)

        val tiltakstype = Tiltakstype(
            id = UUID.randomUUID(),
            navn = "Arbeidstrening",
            tiltakskode = "ARBTREN"
        )

        val tiltaksgjennomforing = Tiltaksgjennomforing(
            id = UUID.randomUUID(),
            navn = "Arbeidstrening",
            tiltakstypeId = tiltakstype.id,
            tiltaksnummer = "12345",
            virksomhetsnummer = "123456789",
            fraDato = LocalDateTime.of(2022, 11, 11, 0, 0),
            tilDato = LocalDateTime.of(2023, 11, 11, 0, 0),
            enhet = "2990"
        )

        val deltaker = Deltaker(
            id = UUID.randomUUID(),
            tiltaksgjennomforingId = tiltaksgjennomforing.id,
            norskIdent = "12345678910",
            status = Deltakerstatus.VENTER,
            fraDato = LocalDateTime.now(),
            tilDato = LocalDateTime.now().plusYears(1)
        )

        test("upsert tiltakstype") {
            val table = Table(database.db.getDatasource(), "tiltakstype")

            service.createOrUpdate(tiltakstype)
            service.createOrUpdate(tiltakstype.copy(navn = "Arbeidsovertrening"))

            assertThat(table).row(0)
                .column("id").value().isEqualTo(tiltakstype.id)
                .column("navn").value().isEqualTo("Arbeidsovertrening")
        }

        test("upsert tiltaksgjennomføring") {
            val table = Table(database.db.getDatasource(), "tiltaksgjennomforing")

            service.createOrUpdate(tiltaksgjennomforing)
            service.createOrUpdate(tiltaksgjennomforing.copy(navn = "Oppdatert arbeidstrening"))

            assertThat(table).row(0)
                .column("id").value().isEqualTo(tiltaksgjennomforing.id)
                .column("navn").value().isEqualTo("Oppdatert arbeidstrening")
                .column("tiltakstype_id").value().isEqualTo(tiltakstype.id)
                .column("tiltaksnummer").value().isEqualTo("12345")
                .column("virksomhetsnummer").value().isEqualTo("123456789")
                .column("fra_dato").value()
                .isEqualTo(LocalDateTime.of(2022, 11, 11, 0, 0))
                .column("til_dato").value()
                .isEqualTo(LocalDateTime.of(2023, 11, 11, 0, 0))
        }

        test("upsert deltaker") {
            val table = Table(database.db.getDatasource(), "deltaker")

            service.createOrUpdate(deltaker)
            service.createOrUpdate(deltaker.copy(status = Deltakerstatus.DELTAR))

            assertThat(table).row(0)
                .column("id").value().isEqualTo(deltaker.id)
                .column("status").value().isEqualTo("DELTAR")
        }
    }
})
