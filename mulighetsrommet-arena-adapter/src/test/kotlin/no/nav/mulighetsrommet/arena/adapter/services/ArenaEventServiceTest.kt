package no.nav.mulighetsrommet.arena.adapter.services

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.kotest.core.spec.style.FunSpec
import io.mockk.coVerify
import io.mockk.spyk
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import no.nav.mulighetsrommet.arena.adapter.databaseConfig
import no.nav.mulighetsrommet.arena.adapter.events.processors.ArenaEventProcessor
import no.nav.mulighetsrommet.arena.adapter.models.ProcessingError
import no.nav.mulighetsrommet.arena.adapter.models.ProcessingResult
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTable
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping.Status.Handled
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping.Status.Ignored
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.ProcessingStatus
import no.nav.mulighetsrommet.arena.adapter.repositories.*
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import java.util.*

class ArenaEventServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    afterEach {
        database.truncateAll()
    }

    val table = ArenaTable.Tiltakstype

    val pendingEvent = ArenaEvent(
        status = ProcessingStatus.Pending,
        arenaTable = table,
        operation = ArenaEvent.Operation.Insert,
        arenaId = "1",
        payload = JsonObject(mapOf("after" to JsonObject(mapOf("name" to JsonPrimitive("Foo"))))),
    )
    val processedEvent = ArenaEvent(
        status = ProcessingStatus.Processed,
        arenaTable = table,
        operation = ArenaEvent.Operation.Insert,
        arenaId = "2",
        payload = JsonObject(mapOf("after" to JsonObject(mapOf("name" to JsonPrimitive("Bar"))))),
    )
    val failedEvent = ArenaEvent(
        status = ProcessingStatus.Failed,
        arenaTable = table,
        operation = ArenaEvent.Operation.Insert,
        arenaId = "3",
        payload = JsonObject(mapOf("after" to JsonObject(mapOf("name" to JsonPrimitive("Baz"))))),
    )
    val eksternId = UUID.randomUUID()
    val pendingEventWithEksternId = ArenaEvent(
        status = ProcessingStatus.Pending,
        arenaTable = table,
        operation = ArenaEvent.Operation.Insert,
        arenaId = "4",
        payload = JsonObject(mapOf("after" to JsonObject(mapOf("EKSTERN_ID" to JsonPrimitive(eksternId.toString()))))),
    )

    lateinit var events: ArenaEventRepository
    lateinit var entities: ArenaEntityService
    lateinit var entitiesRepository: ArenaEntityMappingRepository

    beforeEach {
        events = ArenaEventRepository(database.db)
        entitiesRepository = ArenaEntityMappingRepository(db = database.db)
        entities = ArenaEntityService(
            mappings = entitiesRepository,
            tiltakstyper = TiltakstypeRepository(db = database.db),
            saker = SakRepository(db = database.db),
            tiltaksgjennomforinger = TiltaksgjennomforingRepository(db = database.db),
            avtaler = AvtaleRepository(db = database.db),
        )
    }

    context("process event") {
        test("should process and save the event") {
            val processor = ArenaEventTestProcessor()

            val service = ArenaEventService(events = events, processors = listOf(processor), entities = entities)
            service.processEvent(pendingEvent)

            database.assertTable("arena_events").row()
                .value("processing_status").isEqualTo(ProcessingStatus.Processed.name)
                .value("message").isNull
        }

        test("should handle multiple processors for the same event") {
            val processor1 = spyk(ArenaEventTestProcessor())
            val processor2 = spyk(ArenaEventTestProcessor())

            val service = ArenaEventService(
                events = events,
                processors = listOf(processor1, processor2),
                entities = entities,
            )
            service.processEvent(pendingEvent)

            coVerify(exactly = 1) {
                processor1.handleEvent(pendingEvent)
                processor2.handleEvent(pendingEvent)
            }

            database.assertTable("arena_events").row()
                .value("processing_status").isEqualTo(ProcessingStatus.Processed.name)
                .value("message").isNull
        }

        test("should not replay dependent events when event gets processed successfully") {
            val dependentEvent = ArenaEvent(
                status = ProcessingStatus.Processed,
                arenaTable = table,
                operation = ArenaEvent.Operation.Insert,
                arenaId = "5",
                payload = JsonObject(mapOf("after" to JsonObject(mapOf("name" to JsonPrimitive("Dependent Bar"))))),
            )

            val dependentEventMapping = entities.getOrCreateMapping(dependentEvent)
            val processedEventMapping = entities.getOrCreateMapping(processedEvent)

            val processor = spyk(
                ArenaEventTestProcessor(
                    eventIsRelevant = { it == processedEvent },
                    getDependentEntities = { listOf(dependentEventMapping) },
                ) {
                    ProcessingResult(Handled).right()
                },
            )

            val dependentEventProcessor = spyk(
                ArenaEventTestProcessor({ it == dependentEvent }) {
                    ProcessingResult(Handled).right()
                },
            )

            val service = ArenaEventService(
                events = events,
                processors = listOf(processor, dependentEventProcessor),
                entities = entities,
            )

            // Prosesser [dependentEvent] først
            service.processEvent(dependentEvent)

            // Deretter [processedEvent]
            service.processEvent(processedEvent)

            // Verifiser at [processedEVent] blitt prosessert én gang
            coVerify(exactly = 1) {
                processor.handleEvent(processedEvent)
            }
            coVerify(exactly = 0) {
                processor.handleEvent(dependentEvent)
            }

            // Verifiser at [dependentEvent] har blitt prosessert én gang
            coVerify(exactly = 0) {
                dependentEventProcessor.handleEvent(processedEvent)
            }
            coVerify(exactly = 1) {
                dependentEventProcessor.handleEvent(dependentEvent)
            }

            // Verifiser tilstand i underliggende tabeller
            database.assertTable("arena_events")
                .row()
                .value("arena_id").isEqualTo(processedEvent.arenaId)
                .value("processing_status").isEqualTo(ProcessingStatus.Processed.name)
                .value("message").isNull
                .row()
                .value("arena_id").isEqualTo(dependentEvent.arenaId)
                .value("processing_status").isEqualTo(ProcessingStatus.Processed.name)
                .value("message").isNull

            database.assertTable("arena_entity_mapping")
                .row()
                .value("entity_id").isEqualTo(processedEventMapping.entityId)
                .value("status").isEqualTo(Handled.name)
                .value("message").isNull
                .row()
                .value("entity_id").isEqualTo(dependentEventMapping.entityId)
                .value("status").isEqualTo(Handled.name)
                .value("message").isNull
        }

        test("should save the event with an error status when the processor fails to handle the event") {
            val processor = ArenaEventTestProcessor {
                ProcessingError.ProcessingFailed(":(").left()
            }

            val service = ArenaEventService(events = events, processors = listOf(processor), entities = entities)
            service.processEvent(pendingEvent)

            database.assertTable("arena_events").row()
                .value("processing_status").isEqualTo(ProcessingStatus.Failed.name)
                .value("message").isEqualTo("Event processing failed: :(")
        }

        test("should not process the event by a second processor when the first processor fails to handle the event") {
            val processor1 = ArenaEventTestProcessor {
                ProcessingError.ProcessingFailed(":(").left()
            }
            val processor2 = spyk(ArenaEventTestProcessor())

            val service = ArenaEventService(
                events = events,
                processors = listOf(processor1, processor2),
                entities = entities,
            )
            service.processEvent(pendingEvent)

            coVerify(exactly = 0) {
                processor2.handleEvent(any())
            }

            database.assertTable("arena_events").row()
                .value("processing_status").isEqualTo(ProcessingStatus.Failed.name)
                .value("message").isEqualTo("Event processing failed: :(")
        }

        test("should not process the event by a second processor when the first processor marks the entity as Ignored") {
            val processor1 = ArenaEventTestProcessor {
                ProcessingResult(Ignored, ":/").right()
            }
            val processor2 = spyk(ArenaEventTestProcessor())

            val service = ArenaEventService(
                events = events,
                processors = listOf(processor1, processor2),
                entities = entities,
            )
            service.processEvent(pendingEvent)

            coVerify(exactly = 0) {
                processor2.handleEvent(any())
            }

            database.assertTable("arena_events").row()
                .value("processing_status").isEqualTo(ProcessingStatus.Processed.name)
                .value("message").isNull
            database.assertTable("arena_entity_mapping").row()
                .value("status").isEqualTo(Ignored.name)
                .value("message").isEqualTo(":/")
        }

        test("should not process the event when it's rejected by a all processors") {
            val processor = spyk(ArenaEventTestProcessor(eventIsRelevant = { false }))

            val service = ArenaEventService(events = events, processors = listOf(processor), entities = entities)
            service.processEvent(pendingEvent)

            coVerify(exactly = 0) {
                processor.handleEvent(any())
            }

            database.assertTable("arena_events").row()
                .value("processing_status").isEqualTo(ProcessingStatus.Pending.name)
        }

        test("should save the event as Failed when processing fails with an exception") {
            val processor = ArenaEventTestProcessor {
                throw RuntimeException("Oh no!")
            }

            val service = ArenaEventService(events = events, processors = listOf(processor), entities = entities)
            service.processEvent(pendingEvent)

            database.assertTable("arena_events").row()
                .value("processing_status").isEqualTo(ProcessingStatus.Failed.name)
                .value("message").isEqualTo("Oh no!")
        }

        test("should delete the entity if it was upserted but now should be ignored") {
            val processor = spyk(
                ArenaEventTestProcessor {
                    ProcessingResult(Ignored, "test").right()
                },
            )

            val service = ArenaEventService(events = events, processors = listOf(processor), entities = entities)
            entities.getOrCreateMapping(processedEvent)
            service.processEvent(processedEvent)

            coVerify(exactly = 1) {
                processor.deleteEntity(processedEvent)
            }

            database.assertTable("arena_events").row()
                .value("processing_status").isEqualTo(ProcessingStatus.Processed.name)
            database.assertTable("arena_entity_mapping").row()
                .value("status").isEqualTo(Ignored.name)
                .value("message").isEqualTo("test")
        }

        test("should save the event as Failed when delete fails") {
            val processedEventMapping = entities.getOrCreateMapping(processedEvent)

            val processor = spyk(
                ArenaEventTestProcessor(
                    deleteEntityError = { ProcessingError.ProcessingFailed(":(") },
                ) {
                    ProcessingResult(Ignored).right()
                },
            )

            val service = ArenaEventService(events = events, processors = listOf(processor), entities = entities)

            service.processEvent(processedEvent)

            coVerify(exactly = 1) {
                processor.deleteEntity(processedEvent)
            }

            database.assertTable("arena_events").row()
                .value("processing_status").isEqualTo(ProcessingStatus.Failed.name)
                .value("message").isEqualTo("Event processing failed: :(")
            database.assertTable("arena_entity_mapping").row()
                .value("entity_id").isEqualTo(processedEventMapping.entityId)
                .value("status").isEqualTo(Handled.name)
                .value("message").isNull
        }

        test("should replay dependent events when delete fails with a foreign key violation") {
            val dependentEvent = ArenaEvent(
                status = ProcessingStatus.Processed,
                // En vilkårlig tabell som ikke er brukt i [processedEvent]
                arenaTable = ArenaTable.AvtaleInfo,
                operation = ArenaEvent.Operation.Insert,
                arenaId = "5",
                payload = JsonObject(mapOf("after" to JsonObject(mapOf("name" to JsonPrimitive("Dependent Bar"))))),
            )

            val dependentEventMapping = entities.getOrCreateMapping(dependentEvent)
            val processedEventMapping = entities.getOrCreateMapping(processedEvent)

            val processor = spyk(
                ArenaEventTestProcessor(
                    eventIsRelevant = { it == processedEvent },
                    deleteEntityError = { ProcessingError.ForeignKeyViolation(":(") },
                    getDependentEntities = { listOf(dependentEventMapping) },
                ) {
                    ProcessingResult(Ignored, "test").right()
                },
            )

            val dependentEventProcessor = spyk(
                ArenaEventTestProcessor({ it == dependentEvent }) {
                    ProcessingResult(Handled).right()
                },
            )

            val service = ArenaEventService(
                events = events,
                processors = listOf(processor, dependentEventProcessor),
                entities = entities,
            )

            // Prosesser [dependentEvent] først
            service.processEvent(dependentEvent)

            // Deretter [processedEvent]
            service.processEvent(processedEvent)

            // Verifiser at [processedEVent] har blitt forsøkt prosessert
            coVerify(exactly = 1) {
                processor.handleEvent(processedEvent)
            }

            // Verifiser at [dependentEvent] har blitt prosessert en ekstra gang
            coVerify(exactly = 2) {
                dependentEventProcessor.handleEvent(dependentEvent)
            }

            // Verifiser tilstand i underliggende tabeller
            database.assertTable("arena_events")
                .row()
                .value("arena_id").isEqualTo(dependentEvent.arenaId)
                .value("processing_status").isEqualTo(ProcessingStatus.Processed.name)
                .value("message").isNull
                .row()
                .value("arena_id").isEqualTo(processedEvent.arenaId)
                .value("processing_status").isEqualTo(ProcessingStatus.Failed.name)
                .value("message").isEqualTo("Dependent event has not yet been processed: :(")
            database.assertTable("arena_entity_mapping")
                .row()
                .value("entity_id").isEqualTo(dependentEventMapping.entityId)
                .value("status").isEqualTo(Handled.name)
                .value("message").isNull
                .row()
                .value("entity_id").isEqualTo(processedEventMapping.entityId)
                .value("status").isEqualTo(Handled.name)
                .value("message").isNull
        }

        test("should not delete the entity if it was unhandled but now should be ignored") {
            val processor = spyk(
                ArenaEventTestProcessor { ProcessingResult(Ignored).right() },
            )

            val service = ArenaEventService(events = events, processors = listOf(processor), entities = entities)
            entities.getOrCreateMapping(processedEvent)
            service.processEvent(pendingEvent)

            coVerify(exactly = 0) {
                processor.deleteEntity(pendingEvent)
            }

            database.assertTable("arena_events").row()
                .value("processing_status").isEqualTo(ProcessingStatus.Processed.name)
                .value("message").isNull
            database.assertTable("arena_entity_mapping").row()
                .value("status").isEqualTo(Ignored.name)
        }

        test("should use EKSTERN_ID if exists for table Tiltaksgjennomforing") {
            val processor = ArenaEventTestProcessor()

            val service = ArenaEventService(events = events, processors = listOf(processor), entities = entities)
            service.processEvent(pendingEventWithEksternId.copy(arenaTable = ArenaTable.Tiltaksgjennomforing))

            database.assertTable("arena_entity_mapping").row()
                .value("entity_id").isEqualTo(eksternId)
                .value("arena_id").isEqualTo(pendingEventWithEksternId.arenaId)
        }

        test("should not EKSTERN_ID if exists for other tables than Tiltaksgjennomforing") {
            val processor = ArenaEventTestProcessor()

            val service = ArenaEventService(events = events, processors = listOf(processor), entities = entities)
            service.processEvent(pendingEventWithEksternId)

            database.assertTable("arena_entity_mapping").row()
                .value("entity_id").isNotEqualTo(eksternId)
                .value("arena_id").isEqualTo(pendingEventWithEksternId.arenaId)
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
            entities.getOrCreateMapping(pendingEvent)
            events.upsert(processedEvent)
            entities.getOrCreateMapping(processedEvent)
            events.upsert(failedEvent)
            entities.getOrCreateMapping(failedEvent)

            val service = ArenaEventService(events = events, processors = listOf(), entities = entities)
            service.setReplayStatusForEvents(table, ArenaEntityMapping.Status.Handled)

            database.assertTable("arena_events")
                .row().value("processing_status").isEqualTo(ProcessingStatus.Pending.name)
                .row().value("processing_status").isEqualTo(ProcessingStatus.Replay.name)
                .row().value("processing_status").isEqualTo(ProcessingStatus.Failed.name)
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
                processors = listOf(processor),
                entities = entities,
            )
            service.retryEvents(table)

            coVerify(exactly = 0) {
                processor.handleEvent(any())
            }

            database.assertTable("arena_events")
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
                processors = listOf(processor),
                entities = entities,
            )
            service.retryEvents(table)

            coVerify(exactly = 1) {
                processor.handleEvent(any())
            }

            database.assertTable("arena_events")
                .row().value("retries").isEqualTo(1)
                .row().value("retries").isEqualTo(1)
        }
    }

    context("delete entity") {
        test("should call the deleteEntity handler for the events that matches the provided filter") {
            val processor = spyk(ArenaEventTestProcessor())
            events.upsert(pendingEvent)
            events.upsert(processedEvent)
            events.upsert(failedEvent)

            val service = ArenaEventService(events = events, processors = listOf(processor), entities = entities)
            service.deleteEntities(table, listOf(processedEvent.arenaId, failedEvent.arenaId))

            coVerify(exactly = 1) {
                processor.deleteEntity(processedEvent)
                processor.deleteEntity(failedEvent)
            }
        }
    }
})

class ArenaEventTestProcessor(
    private val eventIsRelevant: (ArenaEvent) -> Boolean = { true },
    private val deleteEntityError: (() -> ProcessingError)? = null,
    private val getDependentEntities: (() -> List<ArenaEntityMapping>)? = null,
    private val handleEvent: (() -> Either<ProcessingError, ProcessingResult>)? = null,
) : ArenaEventProcessor {

    override suspend fun shouldHandleEvent(event: ArenaEvent): Boolean {
        return eventIsRelevant(event)
    }

    override suspend fun handleEvent(event: ArenaEvent): Either<ProcessingError, ProcessingResult> {
        return handleEvent?.invoke() ?: Either.Right(ProcessingResult(Handled))
    }

    override suspend fun deleteEntity(event: ArenaEvent): Either<ProcessingError, Unit> {
        return deleteEntityError?.invoke()?.left() ?: Either.Right(Unit)
    }

    override fun getDependentEntities(event: ArenaEvent): List<ArenaEntityMapping> {
        return getDependentEntities?.invoke() ?: emptyList()
    }
}
