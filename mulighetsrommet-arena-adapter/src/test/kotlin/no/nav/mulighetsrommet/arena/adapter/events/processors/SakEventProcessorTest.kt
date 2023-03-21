package no.nav.mulighetsrommet.arena.adapter.events.processors

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.arena.adapter.createDatabaseTestConfig
import no.nav.mulighetsrommet.arena.adapter.fixtures.SakFixtures
import no.nav.mulighetsrommet.arena.adapter.fixtures.createArenaSakEvent
import no.nav.mulighetsrommet.arena.adapter.models.ProcessingResult
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping.Status.Handled
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.Operation.Insert
import no.nav.mulighetsrommet.arena.adapter.repositories.*
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener

class SakEventProcessorTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

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

            consumer.handleEvent(event).shouldBeRight().should { it.status shouldBe ArenaEntityMapping.Status.Ignored }
        }
    }

    context("when sakskode is TILT") {
        test("should treat all operations as upserts") {
            val consumer = createConsumer(database.db)

            val e1 = createArenaSakEvent(Insert) { it.copy(LOPENRSAK = 1) }
            consumer.handleEvent(e1) shouldBeRight ProcessingResult(Handled)
            database.assertThat("sak").row().value("lopenummer").isEqualTo(1)

            val e2 = createArenaSakEvent(Insert) { it.copy(LOPENRSAK = 2) }
            consumer.handleEvent(e2) shouldBeRight ProcessingResult(Handled)
            database.assertThat("sak").row().value("lopenummer").isEqualTo(2)

            val e3 = createArenaSakEvent(Insert) { it.copy(LOPENRSAK = 3) }
            consumer.handleEvent(e3) shouldBeRight ProcessingResult(Handled)
            database.assertThat("sak").row().value("lopenummer").isEqualTo(3)
        }
    }
})

private fun createConsumer(db: Database): SakEventProcessor {
    val entities = ArenaEntityService(
        mappings = ArenaEntityMappingRepository(db),
        tiltakstyper = TiltakstypeRepository(db),
        saker = SakRepository(db),
        tiltaksgjennomforinger = TiltaksgjennomforingRepository(db),
        deltakere = DeltakerRepository(db),
        avtaler = AvtaleRepository(db),
    )

    return SakEventProcessor(entities)
}
