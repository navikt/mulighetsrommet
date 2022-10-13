package no.nav.mulighetsrommet.arena.adapter.no.nav.mulighetsrommet.arena.adapter.repositories

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.collections.shouldHaveSize
import no.nav.mulighetsrommet.arena.adapter.repositories.EventRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseListener
import no.nav.mulighetsrommet.database.kotest.extensions.createArenaAdapterDatabaseTestSchema
import org.assertj.db.api.Assertions
import org.assertj.db.type.Table

class EventRepositoryTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val listener =
        FlywayDatabaseListener(createArenaAdapterDatabaseTestSchema())
    register(listener)

    lateinit var eventRepository: EventRepository
    lateinit var table: Table

    beforeSpec {
        eventRepository = EventRepository(listener.db)
        table = Table(listener.db.getDatasource(), "events")
    }

    test("should save events") {
        (0..4).forEach { eventRepository.upsert("topic", it.toString(), "{}") }

        Assertions.assertThat(table).hasNumberOfRows(5)
    }

    test("should retrieve 3 saved events") {
        val events = eventRepository.getAll("topic", 3)

        events shouldHaveSize 3
    }

    test("should retrieve 3 saved events starting from id 2") {
        val events = eventRepository.getAll("topic", 3, 2)

        events shouldHaveSize 3
        events.map { it.id } shouldContainInOrder listOf(3, 4, 5)
    }
})
