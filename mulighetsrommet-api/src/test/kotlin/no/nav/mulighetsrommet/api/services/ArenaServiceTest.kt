package no.nav.mulighetsrommet.api.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import no.nav.mulighetsrommet.api.repositories.DeltakerRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseListener
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

    val listener = FlywayDatabaseListener(createApiDatabaseTestSchema())

    register(listener)

    context("ArenaService") {
        val tiltakstypeRepository = TiltakstypeRepository(listener.db)
        val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(listener.db)
        val deltakerRepository = DeltakerRepository(listener.db)
        val service = ArenaService(tiltakstypeRepository, tiltaksgjennomforingRepository, deltakerRepository)

        val tiltakstype = Tiltakstype(
            id = UUID.randomUUID(),
            navn = "Arbeidstrening",
            tiltakskode = "ARBTREN",
        )

        val tiltaksgjennomforing = Tiltaksgjennomforing(
            id = UUID.randomUUID(),
            navn = "Arbeidstrening",
            tiltakstypeId = tiltakstype.id,
            tiltaksnummer = "12345"
        )

        val deltaker = Deltaker(
            id = UUID.randomUUID(),
            tiltaksgjennomforingId = tiltaksgjennomforing.id,
            fnr = "12345678910",
            status = Deltakerstatus.VENTER,
            fraDato = LocalDateTime.now(),
            tilDato = LocalDateTime.now().plusYears(1),
            virksomhetsnr = "123456789"
        )

        test("upsert tiltakstype") {
            val table = Table(listener.db.getDatasource(), "tiltakstype")

            service.createOrUpdate(tiltakstype)
            service.createOrUpdate(tiltakstype.copy(navn = "Arbeidsovertrening"))

            assertThat(table).row(0)
                .column("id").value().isEqualTo(1)
                .column("navn").value().isEqualTo("Arbeidsovertrening")
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
    }
})
