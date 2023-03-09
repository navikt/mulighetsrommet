package no.nav.mulighetsrommet.arena.adapter.services

import arrow.core.Either
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.mockk.coVerify
import io.mockk.spyk
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import no.nav.mulighetsrommet.arena.adapter.events.processors.ArenaEventProcessor
import no.nav.mulighetsrommet.arena.adapter.models.ProcessingError
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTable
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.ProcessingStatus
import no.nav.mulighetsrommet.arena.adapter.repositories.*
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

    val pendingEvent = ArenaEvent(
        status = ProcessingStatus.Pending,
        arenaTable = table,
        operation = ArenaEvent.Operation.Insert,
        arenaId = "1",
        payload = JsonObject(mapOf("name" to JsonPrimitive("Foo")))
    )
    val processedEvent = ArenaEvent(
        status = ProcessingStatus.Processed,
        arenaTable = table,
        operation = ArenaEvent.Operation.Insert,
        arenaId = "2",
        payload = JsonObject(mapOf("name" to JsonPrimitive("Bar")))
    )
    val ignoredEvent = ArenaEvent(
        status = ProcessingStatus.Processed,
        arenaTable = table,
        operation = ArenaEvent.Operation.Insert,
        arenaId = "3",
        payload = JsonObject(mapOf("name" to JsonPrimitive("Baz")))
    )

    lateinit var events: ArenaEventRepository
    lateinit var entities: ArenaEntityService

    beforeEach {
        events = ArenaEventRepository(database.db)
        entities = ArenaEntityService(
            mappings = ArenaEntityMappingRepository(db = database.db),
            tiltakstyper = TiltakstypeRepository(db = database.db),
            saker = SakRepository(db = database.db),
            tiltaksgjennomforinger = TiltaksgjennomforingRepository(db = database.db),
            deltakere = DeltakerRepository(db = database.db),
            avtaler = AvtaleRepository(db = database.db),
            events = events
        )
    }

    context("process event") {
        test("should process and save the event") {
            val processor = ArenaEventTestProcessor()

            val service = ArenaEventService(events = events, processors = listOf(processor), entities = entities)
            service.processEvent(pendingEvent)

            database.assertThat("arena_events").row()
                .value("processing_status").isEqualTo(ProcessingStatus.Processed.name)
                .value("message").isNull
        }

        test("should save the event with an error status when the processor fails to handle the event") {
            val processor = ArenaEventTestProcessor {
                ProcessingError.ProcessingFailed(":(")
            }

            val service = ArenaEventService(events = events, processors = listOf(processor), entities = entities)
            service.processEvent(pendingEvent)

            database.assertThat("arena_events").row()
                .value("processing_status").isEqualTo(ProcessingStatus.Failed.name)
                .value("message").isEqualTo("Event processing failed: :(")
        }

        test("should not process the event when it corresponds to a different Arena entity than the processor's configured ArenaTable") {
            val sakEvent = pendingEvent.copy(arenaTable = ArenaTable.Sak)
            val processor = spyk(ArenaEventTestProcessor(arenaTable = ArenaTable.Tiltakstype))

            val service = ArenaEventService(events = events, processors = listOf(processor), entities = entities)
            service.processEvent(sakEvent)

            coVerify(exactly = 0) {
                processor.handleEvent(any())
            }

            database.assertThat("arena_events").row()
                .value("processing_status").isEqualTo(ProcessingStatus.Pending.name)
        }

        test("should save the event as Failed when processing fails with an exception") {
            val processor = ArenaEventTestProcessor {
                throw RuntimeException("Oh no!")
            }

            val service = ArenaEventService(events = events, processors = listOf(processor), entities = entities)
            service.processEvent(pendingEvent)

            database.assertThat("arena_events").row()
                .value("processing_status").isEqualTo(ProcessingStatus.Failed.name)
                .value("message").isEqualTo("Oh no!")
        }
    }

    context("replay event") {
        test("should run gracefully when specified event does not exist") {
            val processor = spyk(ArenaEventTestProcessor())

            val service = ArenaEventService(events = events, processors = listOf(processor), entities = entities)
            service.replayEvent(table, "1")

            coVerify(exactly = 0) {
                processor.handleEvent(any())
            }
        }

        test("should replay event payload specified by id") {
            val processor = spyk(ArenaEventTestProcessor())
            events.upsert(pendingEvent)

            val service = ArenaEventService(events = events, processors = listOf(processor), entities = entities)
            service.replayEvent(table, "1")

            coVerify(exactly = 1) {
                processor.handleEvent(pendingEvent)
            }
        }
    }

    context("replay events") {
        test("should set processing status to Replay for specified table and status") {
            events.upsert(pendingEvent)
            events.upsert(processedEvent)
            events.upsert(ignoredEvent)

            val service = ArenaEventService(events = events, processors = listOf(), entities = entities)
            service.setReplayStatusForEvents(table, ProcessingStatus.Processed)

            database.assertThat("arena_events")
                .row().value("processing_status").isEqualTo(ProcessingStatus.Pending.name)
                .row().value("processing_status").isEqualTo(ProcessingStatus.Replay.name)
                .row().value("processing_status").isEqualTo(ProcessingStatus.Processed.name)
        }
    }

    context("retry events") {
        test("should run gracefully when there are no events to retry") {
            val processor = spyk(ArenaEventTestProcessor())
            events.upsert(pendingEvent)

            val service = ArenaEventService(events = events, processors = listOf(processor), entities = entities)
            service.retryEvents(table)

            coVerify(exactly = 0) {
                processor.handleEvent(any())
            }
        }

        test("should not retry events that has been retried as many times as the configured maxRetries") {
            val processor = spyk(ArenaEventTestProcessor())
            events.upsert(pendingEvent)
            events.upsert(processedEvent)

            val service = ArenaEventService(
                config = ArenaEventService.Config(maxRetries = 0),
                events = events,
                processors = listOf(processor), entities = entities
            )
            service.retryEvents(table)

            coVerify(exactly = 0) {
                processor.handleEvent(any())
            }

            database.assertThat("arena_events")
                .row().value("retries").isEqualTo(0)
                .row().value("retries").isEqualTo(0)
        }

        test("should retry events that has been retried less times than the configured maxRetries") {
            val processor = spyk(ArenaEventTestProcessor())
            events.upsert(pendingEvent.copy(retries = 1))
            events.upsert(processedEvent)

            val service = ArenaEventService(
                config = ArenaEventService.Config(maxRetries = 1),
                events = events,
                processors = listOf(processor), entities = entities
            )
            service.retryEvents(table)

            coVerify(exactly = 1) {
                processor.handleEvent(any())
            }

            database.assertThat("arena_events")
                .row().value("retries").isEqualTo(1)
                .row().value("retries").isEqualTo(1)
        }
    }

    context("delete entity") {
        test("should call the deleteEntity handler for the events that matches the provided filter") {
            val processor = spyk(ArenaEventTestProcessor())
            events.upsert(pendingEvent)
            events.upsert(processedEvent)
            events.upsert(ignoredEvent)

            val service = ArenaEventService(events = events, processors = listOf(processor), entities = entities)
            service.deleteEntities(table, listOf(processedEvent.arenaId, ignoredEvent.arenaId))

            coVerify(exactly = 1) {
                processor.deleteEntity(processedEvent)
                processor.deleteEntity(ignoredEvent)
            }
        }
    }
})

class ArenaEventTestProcessor(
    override val arenaTable: ArenaTable = ArenaTable.Tiltakstype,
    private val error: (() -> ProcessingError)? = null
) : ArenaEventProcessor {

    override suspend fun handleEvent(event: ArenaEvent): Either<ProcessingError, ProcessingStatus> {
        return error
            ?.let { Either.Left(it()) }
            ?: Either.Right(ProcessingStatus.Processed)
    }

    override suspend fun deleteEntity(event: ArenaEvent): Either<ProcessingError, Unit> {
        return error
            ?.let { Either.Left(it()) }
            ?: Either.Right(Unit)
    }
}
