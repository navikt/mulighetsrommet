package no.nav.mulighetsrommet.arena.adapter.repositories

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.collections.shouldHaveSize
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.arena.adapter.databaseConfig
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTable
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping.Status.Handled
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping.Status.Ignored
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.ProcessingStatus.*
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import java.util.*

class ArenaEventRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    afterEach {
        database.db.truncateAll()
    }

    context("ArenaEventRepository") {
        val events = ArenaEventRepository(database.db)
        val mappings = ArenaEntityMappingRepository(database.db)

        beforeEach {
            (1..5).forEach {
                val event = events.upsert(
                    ArenaEvent(
                        arenaTable = ArenaTable.Tiltakstype,
                        arenaId = it.toString(),
                        operation = ArenaEvent.Operation.Insert,
                        payload = Json.parseToJsonElement("{}"),
                        status = Processed,
                    ),
                )
                mappings.upsert(
                    ArenaEntityMapping(
                        arenaTable = event.arenaTable,
                        arenaId = event.arenaId,
                        entityId = UUID.randomUUID(),
                        status = Handled,
                    ),
                )
            }
            (6..10).forEach {
                val event = events.upsert(
                    ArenaEvent(
                        arenaTable = ArenaTable.AvtaleInfo,
                        arenaId = it.toString(),
                        operation = ArenaEvent.Operation.Insert,
                        payload = Json.parseToJsonElement("{}"),
                        status = Pending,
                    ),
                )
                mappings.upsert(
                    ArenaEntityMapping(
                        arenaTable = event.arenaTable,
                        arenaId = event.arenaId,
                        entityId = UUID.randomUUID(),
                        status = Ignored,
                    ),
                )
            }
        }

        test("should save events") {
            database.assertThat("arena_events").hasNumberOfRows(10)
        }

        test("should get events specified by table") {
            events.getAll(table = ArenaTable.Tiltakstype) shouldHaveSize 5
            events.getAll(table = ArenaTable.AvtaleInfo) shouldHaveSize 5
        }

        test("should get events specified by status") {
            events.getAll(status = Processed) shouldHaveSize 5
            events.getAll(status = Pending) shouldHaveSize 5
        }

        test("should get events specified by limit and id") {
            val upserted = events.getAll(limit = 3, idGreaterThan = "2")

            upserted.map { it.arenaId } shouldContainInOrder listOf("3", "4", "5")
        }

        test("update event status specified by table and the current entity status") {
            events.updateProcessingStatusFromEntityStatus(
                table = ArenaTable.Tiltakstype,
                entityStatus = Handled,
                processingStatus = Replay,
            )

            events.updateProcessingStatusFromEntityStatus(
                table = ArenaTable.AvtaleInfo,
                entityStatus = Handled,
                processingStatus = Replay,
            )

            events.getAll(status = Replay) shouldHaveSize 5
            events.getAll(status = Pending) shouldHaveSize 5
        }
    }
})
