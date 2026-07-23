package no.nav.mulighetsrommet.admin.deltaker

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.admin.testing.TestAdminDatabase
import no.nav.mulighetsrommet.api.domain.deltaker.DeltakerForslag
import no.nav.mulighetsrommet.api.fixtures.DeltakerFixtures
import no.nav.mulighetsrommet.model.DeltakerStatusType
import java.time.LocalDate
import java.util.UUID

class ReplikerDeltakerForslagUseCaseTest : FunSpec({
    test("lagrer forslag som venter på svar når deltakeren finnes") {
        val db = TestAdminDatabase()
        val deltaker = DeltakerFixtures.createDeltaker(
            gjennomforingId = UUID.randomUUID(),
            status = DeltakerStatusType.DELTAR,
        )
        db.repository.deltaker.save(deltaker)

        val forslag = DeltakerForslag(
            id = UUID.randomUUID(),
            deltakerId = deltaker.id,
            endring = DeltakerForslag.Endring.Sluttdato(sluttdato = LocalDate.now()),
            status = DeltakerForslag.Status.VENTER_PA_SVAR,
        )

        val replikerForslag = ReplikerDeltakerForslagUseCase(db)

        replikerForslag.execute(ReplikerDeltakerForslag(forslag.id, forslag))
            .shouldBe(ReplikerDeltakerForslagResultat.Lagret(deltaker.gjennomforingId))

        db.repository.deltakerForslag.get(forslag.id) shouldBe forslag
    }

    test("lagrer ikke forslag når deltakeren ikke finnes") {
        val db = TestAdminDatabase()

        val forslag = DeltakerForslag(
            id = UUID.randomUUID(),
            deltakerId = UUID.randomUUID(),
            endring = DeltakerForslag.Endring.Sluttdato(sluttdato = LocalDate.now()),
            status = DeltakerForslag.Status.VENTER_PA_SVAR,
        )

        val replikerForslag = ReplikerDeltakerForslagUseCase(db)

        replikerForslag.execute(ReplikerDeltakerForslag(forslag.id, forslag))
            .shouldBe(ReplikerDeltakerForslagResultat.IngenEndring)

        db.repository.deltakerForslag.get(forslag.id).shouldBeNull()
    }

    test("sletter forslag ved tombstone (forslag = null)") {
        val db = TestAdminDatabase()
        val deltaker = DeltakerFixtures.createDeltaker(
            gjennomforingId = UUID.randomUUID(),
            status = DeltakerStatusType.DELTAR,
        )
        db.repository.deltaker.save(deltaker)

        val forslag = DeltakerForslag(
            id = UUID.randomUUID(),
            deltakerId = deltaker.id,
            endring = DeltakerForslag.Endring.Sluttdato(sluttdato = LocalDate.now()),
            status = DeltakerForslag.Status.VENTER_PA_SVAR,
        )
        db.repository.deltakerForslag.save(forslag)

        val replikerForslag = ReplikerDeltakerForslagUseCase(db)

        replikerForslag.execute(ReplikerDeltakerForslag(forslag.id, null))
            .shouldBe(ReplikerDeltakerForslagResultat.Slettet(deltaker.gjennomforingId))

        db.repository.deltakerForslag.getByGjennomforing(deltaker.gjennomforingId).shouldBeEmpty()
    }

    test("tombstone på forslag som ikke finnes fra før gir ingen endring") {
        val db = TestAdminDatabase()
        val replikerForslag = ReplikerDeltakerForslagUseCase(db)

        replikerForslag.execute(ReplikerDeltakerForslag(UUID.randomUUID(), null))
            .shouldBe(ReplikerDeltakerForslagResultat.IngenEndring)
    }

    listOf(
        DeltakerForslag.Status.GODKJENT,
        DeltakerForslag.Status.AVVIST,
        DeltakerForslag.Status.TILBAKEKALT,
        DeltakerForslag.Status.ERSTATTET,
    ).forEach { avsluttetStatus ->
        test("sletter forslag når det er avgjort med status $avsluttetStatus") {
            val db = TestAdminDatabase()
            val deltaker = DeltakerFixtures.createDeltaker(
                gjennomforingId = UUID.randomUUID(),
                status = DeltakerStatusType.DELTAR,
            )
            db.repository.deltaker.save(deltaker)

            val forslag = DeltakerForslag(
                id = UUID.randomUUID(),
                deltakerId = deltaker.id,
                endring = DeltakerForslag.Endring.Sluttdato(sluttdato = LocalDate.now()),
                status = DeltakerForslag.Status.VENTER_PA_SVAR,
            )
            db.repository.deltakerForslag.save(forslag)

            val avgjortForslag = forslag.copy(status = avsluttetStatus)

            val replikerForslag = ReplikerDeltakerForslagUseCase(db)

            replikerForslag.execute(ReplikerDeltakerForslag(forslag.id, avgjortForslag))
                .shouldBe(ReplikerDeltakerForslagResultat.Slettet(deltaker.gjennomforingId))

            db.repository.deltakerForslag.getByGjennomforing(deltaker.gjennomforingId)
                .shouldNotContainKey(deltaker.id)
        }
    }
})
