package no.nav.mulighetsrommet.admin.deltaker

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.admin.testing.TestAdminDatabase
import no.nav.mulighetsrommet.api.fixtures.DeltakerFixtures
import no.nav.mulighetsrommet.model.DeltakerStatusType
import java.time.LocalDateTime
import java.util.UUID

class ReplikerDeltakerUseCaseTest : FunSpec({
    test("lagrer ny deltaker") {
        val db = TestAdminDatabase()
        val replikerDeltaker = ReplikerDeltakerUseCase(db)

        val deltaker = DeltakerFixtures.createDeltaker(gjennomforingId = UUID.randomUUID())

        replikerDeltaker.execute(ReplikerDeltaker(deltaker.id, deltaker))
            .shouldBe(ReplikerDeltakerResultat.Lagret(deltaker.gjennomforingId))

        db.repository.deltaker.get(deltaker.id).shouldNotBeNull().should {
            it.id shouldBe deltaker.id
        }
    }

    test("sletter deltaker ved tombstone (deltaker = null)") {
        val db = TestAdminDatabase()
        val eksisterende = DeltakerFixtures.createDeltaker(gjennomforingId = UUID.randomUUID())
        db.repository.deltaker.save(eksisterende)

        val replikerDeltaker = ReplikerDeltakerUseCase(db)

        replikerDeltaker.execute(ReplikerDeltaker(eksisterende.id, null))
            .shouldBe(ReplikerDeltakerResultat.Slettet(eksisterende.gjennomforingId))

        db.repository.deltaker.get(eksisterende.id).shouldBeNull()
    }

    test("tombstone på deltaker som ikke finnes fra før gir ingen endring") {
        val db = TestAdminDatabase()
        val replikerDeltaker = ReplikerDeltakerUseCase(db)

        replikerDeltaker.execute(ReplikerDeltaker(UUID.randomUUID(), null))
            .shouldBe(ReplikerDeltakerResultat.IngenEndring)
    }

    test("sletter deltaker med status FEILREGISTRERT") {
        val db = TestAdminDatabase()
        val eksisterende = DeltakerFixtures.createDeltaker(gjennomforingId = UUID.randomUUID())
        db.repository.deltaker.save(eksisterende)

        val feilregistrert = eksisterende.copy(
            status = eksisterende.status.copy(type = DeltakerStatusType.FEILREGISTRERT),
        )

        val replikerDeltaker = ReplikerDeltakerUseCase(db)

        replikerDeltaker.execute(ReplikerDeltaker(eksisterende.id, feilregistrert))
            .shouldBe(ReplikerDeltakerResultat.Slettet(feilregistrert.gjennomforingId))

        db.repository.deltaker.get(eksisterende.id).shouldBeNull()
    }

    test("overskriver ikke deltaker når tidspunkt for endring er eldre enn det som er lagret i databasen") {
        val db = TestAdminDatabase()

        val nyereEndring = LocalDateTime.of(2023, 3, 1, 0, 0, 0)
        val eldreEndring = LocalDateTime.of(2023, 2, 1, 0, 0, 0)

        val id = UUID.randomUUID()
        val lagretDeltaker = DeltakerFixtures.createDeltaker(
            id = id,
            gjennomforingId = UUID.randomUUID(),
            status = DeltakerStatusType.DELTAR,
            endretTidspunkt = nyereEndring,
        )
        db.repository.deltaker.save(lagretDeltaker)

        val eldreDeltaker = lagretDeltaker.copy(
            status = lagretDeltaker.status.copy(type = DeltakerStatusType.AVBRUTT),
            endretTidspunkt = eldreEndring,
        )

        val replikerDeltaker = ReplikerDeltakerUseCase(db)

        replikerDeltaker.execute(ReplikerDeltaker(id, eldreDeltaker))
            .shouldBe(ReplikerDeltakerResultat.IngenEndring)

        db.repository.deltaker.get(id).shouldNotBeNull().should {
            it.status.type shouldBe DeltakerStatusType.DELTAR
            it.endretTidspunkt shouldBe nyereEndring
        }
    }

    test("gjør ingenting når deltaker er uendret") {
        val db = TestAdminDatabase()
        val deltaker = DeltakerFixtures.createDeltaker(gjennomforingId = UUID.randomUUID())
        db.repository.deltaker.save(deltaker)

        val replikerDeltaker = ReplikerDeltakerUseCase(db)

        replikerDeltaker.execute(ReplikerDeltaker(deltaker.id, deltaker))
            .shouldBe(ReplikerDeltakerResultat.IngenEndring)
    }

    test("oppdaterer deltaker når tidspunkt for endring er likt, men innholdet er endret") {
        val db = TestAdminDatabase()

        val id = UUID.randomUUID()
        val endretTidspunkt = LocalDateTime.of(2023, 3, 1, 0, 0, 0)
        val deltaker = DeltakerFixtures.createDeltaker(
            id = id,
            gjennomforingId = UUID.randomUUID(),
            status = DeltakerStatusType.DELTAR,
            endretTidspunkt = endretTidspunkt,
        )
        db.repository.deltaker.save(deltaker)

        val oppdatertDeltaker = deltaker.copy(
            status = deltaker.status.copy(type = DeltakerStatusType.AVBRUTT),
            endretTidspunkt = endretTidspunkt,
        )

        val replikerDeltaker = ReplikerDeltakerUseCase(db)

        replikerDeltaker.execute(ReplikerDeltaker(id, oppdatertDeltaker))
            .shouldBe(ReplikerDeltakerResultat.Lagret(oppdatertDeltaker.gjennomforingId))

        db.repository.deltaker.get(id).shouldNotBeNull().should {
            it.status.type shouldBe DeltakerStatusType.AVBRUTT
        }
    }
})
