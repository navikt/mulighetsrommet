package no.nav.mulighetsrommet.arena.adapter.repositories

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.collections.shouldHaveSize
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.ConsumptionStatus.*
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.createArenaAdapterDatabaseTestSchema

class ArenaEventRepositoryTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val database = extension(FlywayDatabaseTestListener(createArenaAdapterDatabaseTestSchema()))

    lateinit var repository: ArenaEventRepository

    beforeSpec {
        repository = ArenaEventRepository(database.db)
    }

    test("should save events") {
        (1..5).forEach {
            repository.upsert(
                ArenaEvent(
                    arenaTable = "table",
                    arenaId = it.toString(),
                    payload = Json.parseToJsonElement("{}"),
                    status = Processed,
                )
            )
        }

        (6..10).forEach {
            repository.upsert(
                ArenaEvent(
                    arenaTable = "table",
                    arenaId = it.toString(),
                    payload = Json.parseToJsonElement("{}"),
                    status = Pending,
                )
            )
        }

        database.assertThat("arena_events").hasNumberOfRows(10)
    }

    test("should get events specified by table") {
        val events = repository.getAll(table = "table")

        events shouldHaveSize 10
    }

    test("should get events specified by consumption status") {
        repository.getAll(status = Processed) shouldHaveSize 5
        repository.getAll(status = Pending) shouldHaveSize 5
    }

    test("should get events specified by limit and id") {
        val events = repository.getAll(limit = 3, idGreaterThan = "2")

        events.map { it.arenaId } shouldContainInOrder listOf("3", "4", "5")
    }

    test("update events consumption status specified by table and consumption status") {
        val events = repository.updateStatus(table = "table", oldStatus = Processed, newStatus = Replay)

        repository.getAll(status = Replay) shouldHaveSize 5
        repository.getAll(status = Pending) shouldHaveSize 5
    }
})
