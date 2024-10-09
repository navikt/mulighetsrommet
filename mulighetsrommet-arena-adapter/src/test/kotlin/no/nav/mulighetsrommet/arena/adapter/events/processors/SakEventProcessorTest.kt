package no.nav.mulighetsrommet.arena.adapter.events.processors

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.arena.adapter.createDatabaseTestConfig
import no.nav.mulighetsrommet.arena.adapter.fixtures.SakFixtures
import no.nav.mulighetsrommet.arena.adapter.fixtures.createArenaSakEvent
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping.Status.Handled
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.Operation.Insert
import no.nav.mulighetsrommet.arena.adapter.repositories.*
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll

class SakEventProcessorTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    afterEach {
        database.db.truncateAll()
    }

    context("handleEvent") {
        val entities = ArenaEntityService(
            mappings = ArenaEntityMappingRepository(database.db),
            tiltakstyper = TiltakstypeRepository(database.db),
            saker = SakRepository(database.db),
            tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db),
            avtaler = AvtaleRepository(database.db),
        )

        context("when sakskode is not TILT") {
            test("should ignore events") {
                val processor = SakEventProcessor(entities)
                val event = createArenaSakEvent(Insert, SakFixtures.ArenaIkkeTiltakSak)

                processor.handleEvent(event).shouldBeRight()
                    .should { it.status shouldBe ArenaEntityMapping.Status.Ignored }
            }
        }

        context("when sakskode is TILT") {
            test("should treat all operations as upserts") {
                val processor = SakEventProcessor(entities)

                val e1 = createArenaSakEvent(Insert) { it.copy(LOPENRSAK = 1) }
                processor.handleEvent(e1).shouldBeRight().should { it.status shouldBe Handled }
                database.assertThat("sak").row().value("lopenummer").isEqualTo(1)

                val e2 = createArenaSakEvent(Insert) { it.copy(LOPENRSAK = 2) }
                processor.handleEvent(e2).shouldBeRight().should { it.status shouldBe Handled }
                database.assertThat("sak").row().value("lopenummer").isEqualTo(2)

                val e3 = createArenaSakEvent(Insert) { it.copy(LOPENRSAK = 3) }
                processor.handleEvent(e3).shouldBeRight().should { it.status shouldBe Handled }
                database.assertThat("sak").row().value("lopenummer").isEqualTo(3)
            }
        }
    }
})
