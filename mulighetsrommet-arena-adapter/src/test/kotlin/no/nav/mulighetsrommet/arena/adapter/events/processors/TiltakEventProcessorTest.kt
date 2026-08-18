package no.nav.mulighetsrommet.arena.adapter.events.processors

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.arena.adapter.databaseConfig
import no.nav.mulighetsrommet.arena.adapter.fixtures.createArenaTiltakEvent
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping.Status.Handled
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.Operation.Delete
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.Operation.Insert
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.Operation.Update
import no.nav.mulighetsrommet.arena.adapter.repositories.ArenaEntityMappingRepository
import no.nav.mulighetsrommet.arena.adapter.repositories.SakRepository
import no.nav.mulighetsrommet.arena.adapter.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.arena.adapter.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener

class TiltakEventProcessorTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    afterEach {
        database.truncateAll()
    }

    context("handleEvent") {
        val tiltakstyper = TiltakstypeRepository(database.db)
        val entities = ArenaEntityService(
            mappings = ArenaEntityMappingRepository(database.db),
            tiltakstyper = tiltakstyper,
            saker = SakRepository(database.db),
            tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db),
        )

        fun createProcessor(): TiltakEventProcessor {
            return TiltakEventProcessor(entities)
        }

        fun prepareEvent(event: ArenaEvent): Pair<ArenaEvent, ArenaEntityMapping> {
            val mapping = entities.getOrCreateMapping(event)
            return Pair(event, mapping)
        }

        test("should treat all operations as upserts") {
            val processor = createProcessor()

            val (e1, mapping) = prepareEvent(createArenaTiltakEvent(Insert) { it.copy(TILTAKSNAVN = "Oppfølging 1") })
            processor.handleEvent(e1).shouldBeRight().should { it.status shouldBe Handled }
            tiltakstyper.get(mapping.entityId)?.navn shouldBe "Oppfølging 1"

            val e2 = createArenaTiltakEvent(Update) { it.copy(TILTAKSNAVN = "Oppfølging 2") }
            processor.handleEvent(e2).shouldBeRight().should { it.status shouldBe Handled }
            tiltakstyper.get(mapping.entityId)?.navn shouldBe "Oppfølging 2"

            val e3 = createArenaTiltakEvent(Delete) { it.copy(TILTAKSNAVN = "Oppfølging 1") }
            processor.handleEvent(e3).shouldBeRight().should { it.status shouldBe Handled }
            tiltakstyper.get(mapping.entityId)?.navn shouldBe "Oppfølging 1"
        }
    }
})
