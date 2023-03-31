package no.nav.mulighetsrommet.api.repositories

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.fixtures.DeltakerFixture
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.utils.getOrThrow
import no.nav.mulighetsrommet.domain.dbo.DeltakerDbo
import no.nav.mulighetsrommet.domain.dbo.Deltakeropphav
import no.nav.mulighetsrommet.domain.dbo.Deltakerstatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class DeltakerRepositoryTest : FunSpec({

    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    beforeEach {
        database.db.clean()
        database.db.migrate()
    }

    context("consume deltakere") {
        beforeTest {
            database.db.migrate()

            val tiltak = TiltakstypeRepository(database.db)
            tiltak.upsert(TiltakstypeFixtures.Oppfolging).getOrThrow()

            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
            tiltaksgjennomforinger.upsert(TiltaksgjennomforingFixtures.Oppfolging1).getOrThrow()
            tiltaksgjennomforinger.upsert(TiltaksgjennomforingFixtures.Oppfolging2).getOrThrow()
        }

        val deltakere = DeltakerRepository(database.db)

        val deltaker1 = DeltakerDbo(
            id = UUID.randomUUID(),
            tiltaksgjennomforingId = TiltaksgjennomforingFixtures.Oppfolging1.id,
            status = Deltakerstatus.VENTER,
            opphav = Deltakeropphav.AMT,
            startDato = null,
            sluttDato = null,
            registrertDato = LocalDateTime.of(2023, 3, 1, 0, 0, 0),
        )
        val deltaker2 = deltaker1.copy(
            id = UUID.randomUUID(),
            tiltaksgjennomforingId = TiltaksgjennomforingFixtures.Oppfolging2.id,
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

    context("Nøkkeltall") {
        val tiltakstypeRepository = TiltakstypeRepository(database.db)
        val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)
        val deltakerRepository = DeltakerRepository(database.db)

        test("Skal telle korrekt antall deltakere for en tiltakstype hittil i år") {
            val tiltakstype = TiltakstypeFixtures.Oppfolging
            val tiltakstype2 = TiltakstypeFixtures.Oppfolging.copy(id = UUID.randomUUID())

            val deltaker1 = DeltakerFixture.Deltaker
            val deltaker2 = DeltakerFixture.Deltaker.copy(id = UUID.randomUUID())
            val deltaker3 = DeltakerFixture.Deltaker.copy(id = UUID.randomUUID())
            val deltaker4 = DeltakerFixture.Deltaker.copy(
                id = UUID.randomUUID(),
                startDato = LocalDate.of(2022, 1, 1),
                sluttDato = LocalDate.of(2022, 12, 12),
            )

            val deltakerPaAnnenTiltaksgjennomforing = DeltakerFixture.Deltaker.copy(
                id = UUID.randomUUID(),
                tiltaksgjennomforingId = TiltaksgjennomforingFixtures.Oppfolging2.id,
            )

            tiltakstypeRepository.upsert(tiltakstype)
            tiltakstypeRepository.upsert(tiltakstype2)
            tiltaksgjennomforingRepository.upsert(TiltaksgjennomforingFixtures.Oppfolging1).getOrThrow()
            tiltaksgjennomforingRepository.upsert(TiltaksgjennomforingFixtures.Oppfolging2.copy(tiltakstypeId = tiltakstype2.id))
                .getOrThrow()
            deltakerRepository.upsert(deltaker1)
            deltakerRepository.upsert(deltaker2)
            deltakerRepository.upsert(deltaker3)
            deltakerRepository.upsert(deltaker4)
            deltakerRepository.upsert(deltakerPaAnnenTiltaksgjennomforing)

            val alleDeltakere = deltakerRepository.getAll(TiltaksgjennomforingFixtures.Oppfolging1.id)
            alleDeltakere.size shouldBe 4

            val antallDeltakere = deltakerRepository.countAntallDeltakereForTiltakstypeWithId(
                tiltakstype.id,
                currentDate = LocalDate.of(2023, 3, 16)
            )
            antallDeltakere shouldBe 3
        }
    }
})
