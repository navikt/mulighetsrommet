package no.nav.mulighetsrommet.arena.adapter.events.processors

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.arena.adapter.fixtures.SakFixtures
import no.nav.mulighetsrommet.arena.adapter.fixtures.createArenaSakEvent
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.Operation.Insert
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.ProcessingStatus.Ignored
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.ProcessingStatus.Processed
import no.nav.mulighetsrommet.arena.adapter.repositories.*
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.createArenaAdapterDatabaseTestSchema

class SakEventProcessorTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val database = extension(FlywayDatabaseTestListener(createArenaAdapterDatabaseTestSchema()))

    beforeEach {
        database.db.migrate()
    }

    afterEach {
        database.db.clean()
    }

    context("when sakskode is not TILT") {
        test("should ignore events") {
            val consumer = createConsumer(database.db)
            val event = createArenaSakEvent(Insert, SakFixtures.ArenaIkkeTiltakSak)

            consumer.handleEvent(event).shouldBeLeft().should { it.status shouldBe Ignored }
        }
    }

    context("when sakskode is TILT") {
        test("should treat all operations as upserts") {
            val consumer = createConsumer(database.db)

            val e1 = createArenaSakEvent(Insert) { it.copy(LOPENRSAK = 1) }
            consumer.handleEvent(e1) shouldBeRight Processed
            database.assertThat("sak").row().value("lopenummer").isEqualTo(1)

            val e2 = createArenaSakEvent(Insert) { it.copy(LOPENRSAK = 2) }
            consumer.handleEvent(e2) shouldBeRight Processed
            database.assertThat("sak").row().value("lopenummer").isEqualTo(2)

            val e3 = createArenaSakEvent(Insert) { it.copy(LOPENRSAK = 3) }
            consumer.handleEvent(e3) shouldBeRight Processed
            database.assertThat("sak").row().value("lopenummer").isEqualTo(3)
        }
    }
})

private fun createConsumer(db: Database): SakEventProcessor {
    val entities = ArenaEntityService(
        events = ArenaEventRepository(db),
        mappings = ArenaEntityMappingRepository(db),
        tiltakstyper = TiltakstypeRepository(db),
        saker = SakRepository(db),
        tiltaksgjennomforinger = TiltaksgjennomforingRepository(db),
        deltakere = DeltakerRepository(db),
        avtaler = AvtaleRepository(db),
    )

    return SakEventProcessor(entities)
}
