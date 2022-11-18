package no.nav.mulighetsrommet.arena.adapter.repositories

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.collections.shouldHaveSize
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseListener
import no.nav.mulighetsrommet.database.kotest.extensions.createArenaAdapterDatabaseTestSchema
import org.assertj.db.api.Assertions
import org.assertj.db.type.Table

class ArenaEventRepositoryTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val listener = extension(FlywayDatabaseListener(createArenaAdapterDatabaseTestSchema()))

    lateinit var repository: ArenaEventRepository
    lateinit var table: Table

    beforeSpec {
        repository = ArenaEventRepository(listener.db)
        table = Table(listener.db.getDatasource(), "arena_events")
    }

    test("should save events") {
        (1..5).forEach {
            repository.upsert(
                ArenaEvent(
                    arenaTable = "table",
                    arenaId = it.toString(),
                    payload = Json.parseToJsonElement("{}"),
                    status = ArenaEvent.ConsumptionStatus.Processed,
                )
            )
        }

        Assertions.assertThat(table).hasNumberOfRows(5)
    }

    test("should retrieve 3 saved events") {
        val events = repository.getAll(table = "table", limit = 3)

        events shouldHaveSize 3
    }

    test("should retrieve 3 saved events starting from id 2") {
        val events = repository.getAll("table", 3, "2")

        events shouldHaveSize 3
        events.map { it.arenaId } shouldContainInOrder listOf("3", "4", "5")
    }
})
