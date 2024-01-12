package no.nav.mulighetsrommet.arena.adapter.services

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.kotest.core.spec.style.FunSpec
import io.mockk.coVerify
import io.mockk.spyk
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import no.nav.mulighetsrommet.arena.adapter.createDatabaseTestConfig
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
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import java.util.*

class ArenaEventServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    afterEach {
        database.db.truncateAll()
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
    val invalidEvent = ArenaEvent(
        status = ProcessingStatus.Invalid,
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
            deltakere = DeltakerRepository(db = database.db),
            avtaler = AvtaleRepository(db = database.db),
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
                ProcessingError.ProcessingFailed(":(").left()
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

            database.assertThat("arena_events").row()
                .value("processing_status").isEqualTo(ProcessingStatus.Processed.name)
            database.assertThat("arena_entity_mapping").row()
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

            database.assertThat("arena_events").row()
                .value("processing_status").isEqualTo(ProcessingStatus.Failed.name)
                .value("message").isEqualTo("Event processing failed: :(")
            database.assertThat("arena_entity_mapping").row()
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
                    deleteEntityError = { ProcessingError.ForeignKeyViolation(":(") },
                    getDependentEntities = { listOf(dependentEventMapping) },
                ) {
                    ProcessingResult(Ignored, "test").right()
                },
            )

            val dependentEventProcessor = spyk(
                ArenaEventTestProcessor(arenaTable = dependentEvent.arenaTable) {
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
            database.assertThat("arena_events")
                .row()
                .value("arena_id").isEqualTo(dependentEvent.arenaId)
                .value("processing_status").isEqualTo(ProcessingStatus.Processed.name)
                .value("message").isNull
                .row()
                .value("arena_id").isEqualTo(processedEvent.arenaId)
                .value("processing_status").isEqualTo(ProcessingStatus.Failed.name)
                .value("message").isEqualTo("Dependent event has not yet been processed: :(")
            database.assertThat("arena_entity_mapping")
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

            database.assertThat("arena_events").row()
                .value("processing_status").isEqualTo(ProcessingStatus.Processed.name)
                .value("message").isNull
            database.assertThat("arena_entity_mapping").row()
                .value("status").isEqualTo(Ignored.name)
        }

        test("should use EKSTERN_ID if exists") {
            val processor = ArenaEventTestProcessor()

            val service = ArenaEventService(events = events, processors = listOf(processor), entities = entities)
            service.processEvent(pendingEventWithEksternId)

            database.assertThat("arena_entity_mapping").row()
                .value("entity_id").isEqualTo(eksternId)
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
            events.upsert(invalidEvent)
            entities.getOrCreateMapping(invalidEvent)

            val service = ArenaEventService(events = events, processors = listOf(), entities = entities)
            service.setReplayStatusForEvents(table, ArenaEntityMapping.Status.Handled)

            database.assertThat("arena_events")
                .row().value("processing_status").isEqualTo(ProcessingStatus.Pending.name)
                .row().value("processing_status").isEqualTo(ProcessingStatus.Replay.name)
                .row().value("processing_status").isEqualTo(ProcessingStatus.Invalid.name)
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
                processors = listOf(processor),
                entities = entities,
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
            events.upsert(invalidEvent)

            val service = ArenaEventService(events = events, processors = listOf(processor), entities = entities)
            service.deleteEntities(table, listOf(processedEvent.arenaId, invalidEvent.arenaId))

            coVerify(exactly = 1) {
                processor.deleteEntity(processedEvent)
                processor.deleteEntity(invalidEvent)
            }
        }
    }
})

class ArenaEventTestProcessor(
    override val arenaTable: ArenaTable = ArenaTable.Tiltakstype,
    private val deleteEntityError: (() -> ProcessingError)? = null,
    private val getDependentEntities: (() -> List<ArenaEntityMapping>)? = null,
    private val handleEvent: (() -> Either<ProcessingError, ProcessingResult>)? = null,
) : ArenaEventProcessor {

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
