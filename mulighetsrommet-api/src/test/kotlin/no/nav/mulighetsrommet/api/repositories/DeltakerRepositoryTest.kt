package no.nav.mulighetsrommet.api.repositories

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.collections.shouldContainExactly
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.utils.getOrThrow
import no.nav.mulighetsrommet.domain.dbo.DeltakerDbo
import no.nav.mulighetsrommet.domain.dbo.Deltakerstatus
import java.time.LocalDateTime
import java.util.*

class DeltakerRepositoryTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    context("consume deltakere") {
        beforeTest {
            database.db.migrate()

            val tiltak = TiltakstypeRepository(database.db)
            tiltak.upsert(TiltakstypeFixtures.Oppfolging).getOrThrow()

            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
            tiltaksgjennomforinger.upsert(TiltaksgjennomforingFixtures.Oppfolging1).getOrThrow()
            tiltaksgjennomforinger.upsert(TiltaksgjennomforingFixtures.Oppfolging2).getOrThrow()
        }

        afterTest {
            database.db.clean()
        }

        val deltakere = DeltakerRepository(database.db)

        val deltaker1 = DeltakerDbo(
            id = UUID.randomUUID(),
            tiltaksgjennomforingId = TiltaksgjennomforingFixtures.Oppfolging1.id,
            norskIdent = "10101010100",
            status = Deltakerstatus.VENTER,
            startDato = null,
            sluttDato = null,
            registrertDato = LocalDateTime.of(2023, 3, 1, 0, 0, 0),
        )
        val deltaker2 = deltaker1.copy(
            id = UUID.randomUUID(),
            tiltaksgjennomforingId = TiltaksgjennomforingFixtures.Oppfolging2.id,
            norskIdent = "10101010101"
        )

        test("CRUD") {
            deltakere.upsert(deltaker1)
            deltakere.upsert(deltaker2)

            deltakere.getAll().shouldContainExactly(deltaker1, deltaker2)

            val avsluttetDeltaker2 = deltaker2.copy(status = Deltakerstatus.AVSLUTTET)
            deltakere.upsert(avsluttetDeltaker2)

            deltakere.getAll().shouldContainExactly(deltaker1, avsluttetDeltaker2)

            deltakere.delete(deltaker1.id)

            deltakere.getAll().shouldContainExactly(avsluttetDeltaker2)
        }

        test("get by tiltaksgjennomforing") {
            deltakere.upsert(deltaker1)
            deltakere.upsert(deltaker2)

            deltakere
                .getAll(tiltaksgjennomforingId = TiltaksgjennomforingFixtures.Oppfolging1.id)
                .shouldContainExactly(deltaker1)

            deltakere
                .getAll(tiltaksgjennomforingId = TiltaksgjennomforingFixtures.Oppfolging2.id)
                .shouldContainExactly(deltaker2)
        }
    }
})
