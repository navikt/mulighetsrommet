package no.nav.mulighetsrommet.arena.adapter.services

import arrow.core.Either
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.mockk.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import no.nav.mulighetsrommet.arena.adapter.events.processors.ArenaEventProcessor
import no.nav.mulighetsrommet.arena.adapter.kafka.ConsumerGroup
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTable
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.ProcessingStatus
import no.nav.mulighetsrommet.arena.adapter.repositories.ArenaEventRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.createArenaAdapterDatabaseTestSchema

class ArenaEventServiceTest : FunSpec({
    testOrder = TestCaseOrder.Sequential

    val database = extension(FlywayDatabaseTestListener(createArenaAdapterDatabaseTestSchema()))

    beforeEach {
        database.db.migrate()
    }

    afterEach {
        database.db.clean()
    }

    val table = ArenaTable.Tiltakstype

    val fooEvent = ArenaEvent(
        status = ProcessingStatus.Processed,
        arenaTable = table,
        operation = ArenaEvent.Operation.Insert,
        arenaId = "1",
        payload = JsonObject(mapOf("name" to JsonPrimitive("Foo")))
    )
    val barEvent = ArenaEvent(
        status = ProcessingStatus.Processed,
        arenaTable = table,
        operation = ArenaEvent.Operation.Insert,
        arenaId = "2",
        payload = JsonObject(mapOf("name" to JsonPrimitive("Bar")))
    )
    val bazEvent = ArenaEvent(
        status = ProcessingStatus.Ignored,
        operation = ArenaEvent.Operation.Insert,
        arenaTable = table,
        arenaId = "3",
        payload = JsonObject(mapOf("name" to JsonPrimitive("Baz")))
    )

    val consumer = mockk<ArenaEventProcessor>()
    val group = ConsumerGroup(listOf(consumer))

    lateinit var events: ArenaEventRepository

    beforeEach {
        every { consumer.arenaTable } returns table
        coEvery { consumer.handleEvent(any()) } answers { Either.Right(ProcessingStatus.Processed) }

        events = ArenaEventRepository(database.db)
    }

    afterEach {
        clearAllMocks()
    }

    context("replay event") {
        test("should run gracefully when specified event does not exist") {
            val service = ArenaEventService(events = events, group = group)
            service.replayEvent(table, "1")

            coVerify(exactly = 0) {
                consumer.handleEvent(any())
            }
        }

        test("should replay event payload specified by id") {
            events.upsert(fooEvent)

            val service = ArenaEventService(events = events, group = group)
            service.replayEvent(table, "1")

            coVerify(exactly = 1) {
                consumer.handleEvent(fooEvent)
            }
        }
    }

    context("replay events") {
        test("should set processing status to Replay for specified table and status") {
            events.upsert(fooEvent)
            events.upsert(barEvent)
            events.upsert(bazEvent)

            val service = ArenaEventService(events = events, group = group)
            service.setReplayStatusForEvents(table, ProcessingStatus.Processed)

            database.assertThat("arena_events")
                .row().value("processing_status").isEqualTo(ProcessingStatus.Replay.name)
                .row().value("processing_status").isEqualTo(ProcessingStatus.Replay.name)
                .row().value("processing_status").isEqualTo(ProcessingStatus.Ignored.name)
        }
    }

    context("retry events") {
        test("should run gracefully when there are no events to retry") {
            events.upsert(fooEvent)

            val service = ArenaEventService(events = events, group = group)
            service.retryEvents(table)

            coVerify(exactly = 0) {
                consumer.handleEvent(any())
            }
        }

        test("should not retry events that has been retried as many times as the configured maxRetries") {
            events.upsert(fooEvent)
            events.upsert(barEvent)

            val service = ArenaEventService(
                config = ArenaEventService.Config(
                    maxRetries = 0
                ),
                events = events,
                group = ConsumerGroup(listOf(consumer))
            )
            service.retryEvents(table)

            coVerify(exactly = 0) {
                consumer.handleEvent(fooEvent)
                consumer.handleEvent(barEvent)
            }

            database.assertThat("arena_events")
                .row().value("retries").isEqualTo(0)
                .row().value("retries").isEqualTo(0)
        }

        test("should retry events that has been retried less times than the configured maxRetries") {
            events.upsert(fooEvent.copy(retries = 1))
            events.upsert(barEvent)

            val service = ArenaEventService(
                config = ArenaEventService.Config(
                    maxRetries = 1
                ),
                events = events,
                group = ConsumerGroup(listOf(consumer))
            )
            service.retryEvents(table)

            coVerify(exactly = 1) {
                consumer.handleEvent(any())
            }

            database.assertThat("arena_events")
                .row().value("retries").isEqualTo(1)
                .row().value("retries").isEqualTo(1)
        }
    }
})
