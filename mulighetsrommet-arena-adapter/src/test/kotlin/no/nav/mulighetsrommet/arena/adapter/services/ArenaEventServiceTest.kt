package no.nav.mulighetsrommet.arena.adapter.services

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping.Status.Unhandled
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.ProcessingStatus
import no.nav.mulighetsrommet.arena.adapter.repositories.ArenaEntityMappingRepository
import no.nav.mulighetsrommet.arena.adapter.repositories.ArenaEventRepository
import no.nav.mulighetsrommet.arena.adapter.repositories.SakRepository
import no.nav.mulighetsrommet.arena.adapter.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.arena.adapter.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import java.util.UUID

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
        )
    }

    fun persistEvent(event: ArenaEvent, mappingStatus: ArenaEntityMapping.Status) {
        events.upsert(event)
        entities.upsertMapping(entities.getOrCreateMapping(event).copy(status = mappingStatus))
    }

    context("process event") {
        test("should process and save the event") {
            val processor = ArenaEventTestProcessor()

            val service = ArenaEventService(events = events, processors = listOf(processor), entities = entities)
            service.processEvent(pendingEvent)

            events.get(table, pendingEvent.arenaId).let {
                it?.status shouldBe ProcessingStatus.Processed
                it?.message shouldBe null
            }
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

            events.get(table, pendingEvent.arenaId).let {
                it?.status shouldBe ProcessingStatus.Processed
                it?.message shouldBe null
            }
        }

        test("should save the event with an error status when the processor fails to handle the event") {
            val processor = ArenaEventTestProcessor {
                ProcessingError.ProcessingFailed(":(").left()
            }

            val service = ArenaEventService(events = events, processors = listOf(processor), entities = entities)
            service.processEvent(pendingEvent)

            events.get(table, pendingEvent.arenaId).let {
                it?.status shouldBe ProcessingStatus.Failed
                it?.message shouldBe "Event processing failed: :("
            }
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

            events.get(table, pendingEvent.arenaId).let {
                it?.status shouldBe ProcessingStatus.Failed
                it?.message shouldBe "Event processing failed: :("
            }
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

            events.get(table, pendingEvent.arenaId).let {
                it?.status shouldBe ProcessingStatus.Processed
                it?.message shouldBe null
            }
            entitiesRepository.get(table, pendingEvent.arenaId).let {
                it?.status shouldBe Ignored
                it?.message shouldBe ":/"
            }
        }

        test("should not process the event when it's rejected by a all processors") {
            val processor = spyk(ArenaEventTestProcessor(eventIsRelevant = { false }))

            val service = ArenaEventService(events = events, processors = listOf(processor), entities = entities)
            service.processEvent(pendingEvent)

            coVerify(exactly = 0) {
                processor.handleEvent(any())
            }

            events.get(table, pendingEvent.arenaId)?.status shouldBe ProcessingStatus.Pending
        }

        test("should save the event as Failed when processing fails with an exception") {
            val processor = ArenaEventTestProcessor {
                throw RuntimeException("Oh no!")
            }

            val service = ArenaEventService(events = events, processors = listOf(processor), entities = entities)
            service.processEvent(pendingEvent)

            events.get(table, pendingEvent.arenaId).let {
                it?.status shouldBe ProcessingStatus.Failed
                it?.message shouldBe "Oh no!"
            }
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

            events.get(table, processedEvent.arenaId)?.status shouldBe ProcessingStatus.Processed
            entitiesRepository.get(table, processedEvent.arenaId).let {
                it?.status shouldBe Ignored
                it?.message shouldBe "test"
            }
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

            events.get(table, processedEvent.arenaId).let {
                it?.status shouldBe ProcessingStatus.Failed
                it?.message shouldBe "Event processing failed: :("
            }
            entitiesRepository.get(table, processedEvent.arenaId).let {
                it?.entityId shouldBe processedEventMapping.entityId
                it?.status shouldBe Handled
                it?.message shouldBe null
            }
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
            events.get(dependentEvent.arenaTable, dependentEvent.arenaId).let {
                it?.status shouldBe ProcessingStatus.Processed
                it?.message shouldBe null
            }
            events.get(processedEvent.arenaTable, processedEvent.arenaId).let {
                it?.status shouldBe ProcessingStatus.Failed
                it?.message shouldBe "Dependent event has not yet been processed: :("
            }
            entitiesRepository.get(dependentEvent.arenaTable, dependentEvent.arenaId).let {
                it?.entityId shouldBe dependentEventMapping.entityId
                it?.status shouldBe Handled
                it?.message shouldBe null
            }
            entitiesRepository.get(processedEvent.arenaTable, processedEvent.arenaId).let {
                it?.entityId shouldBe processedEventMapping.entityId
                it?.status shouldBe Handled
                it?.message shouldBe null
            }
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

            events.get(table, pendingEvent.arenaId).let {
                it?.status shouldBe ProcessingStatus.Processed
                it?.message shouldBe null
            }
            entitiesRepository.get(table, pendingEvent.arenaId)?.status shouldBe Ignored
        }

        test("should use EKSTERN_ID if exists for table Tiltaksgjennomforing") {
            val processor = ArenaEventTestProcessor()

            val service = ArenaEventService(events = events, processors = listOf(processor), entities = entities)
            service.processEvent(pendingEventWithEksternId.copy(arenaTable = ArenaTable.Tiltaksgjennomforing))

            entitiesRepository.get(ArenaTable.Tiltaksgjennomforing, pendingEventWithEksternId.arenaId).let {
                it?.entityId shouldBe eksternId
                it?.arenaId shouldBe pendingEventWithEksternId.arenaId
            }
        }

        test("should not EKSTERN_ID if exists for other tables than Tiltaksgjennomforing") {
            val processor = ArenaEventTestProcessor()

            val service = ArenaEventService(events = events, processors = listOf(processor), entities = entities)
            service.processEvent(pendingEventWithEksternId)

            entitiesRepository.get(pendingEventWithEksternId.arenaTable, pendingEventWithEksternId.arenaId).let {
                it?.entityId shouldNotBe eksternId
                it?.arenaId shouldBe pendingEventWithEksternId.arenaId
            }
        }
    }

    context("dependent event reconciliation") {
        val parentEvent = processedEvent.copy(arenaId = "10")
        val childEvent = processedEvent.copy(arenaTable = ArenaTable.AvtaleInfo, arenaId = "11")
        val otherChildEvent = childEvent.copy(arenaId = "12")

        fun createChildProcessor() = spyk(
            ArenaEventTestProcessor(eventIsRelevant = { it.arenaTable == childEvent.arenaTable }) {
                entities.isIgnored(parentEvent.arenaTable, parentEvent.arenaId).map { isIgnored ->
                    ProcessingResult(if (isIgnored) Ignored else Handled)
                }
            },
        )

        fun createParentProcessor(
            getDependents: () -> List<ArenaEntityMapping> = {
                listOfNotNull(
                    entitiesRepository.get(childEvent.arenaTable, childEvent.arenaId),
                    entitiesRepository.get(otherChildEvent.arenaTable, otherChildEvent.arenaId),
                )
            },
        ) = ArenaEventTestProcessor(
            eventIsRelevant = { it.arenaTable == parentEvent.arenaTable },
            getDependentEntities = getDependents,
        )

        fun createService(processors: List<ArenaEventProcessor>) = ArenaEventService(
            config = ArenaEventService.Config(maxRetries = 10),
            events = events,
            processors = processors,
            entities = entities,
        )

        test("should save the parent status before replaying ignored dependent events") {
            persistEvent(parentEvent, mappingStatus = Ignored)
            persistEvent(childEvent, mappingStatus = Ignored)
            val service = createService(listOf(createParentProcessor(), createChildProcessor()))

            service.processEvent(parentEvent)

            entitiesRepository.get(parentEvent.arenaTable, parentEvent.arenaId)?.status shouldBe Handled
            entitiesRepository.get(childEvent.arenaTable, childEvent.arenaId)?.status shouldBe Handled
            events.get(childEvent.arenaTable, childEvent.arenaId)?.status shouldBe ProcessingStatus.Processed
        }

        test("should resume partially completed reconciliation when the parent is already Handled") {
            persistEvent(parentEvent.copy(status = ProcessingStatus.Failed), mappingStatus = Handled)
            persistEvent(childEvent, mappingStatus = Handled)
            persistEvent(otherChildEvent, mappingStatus = Ignored)
            val childProcessor = createChildProcessor()
            val service = createService(listOf(createParentProcessor(), childProcessor))

            service.retryEvents(table = parentEvent.arenaTable, status = ProcessingStatus.Failed)

            coVerify(exactly = 0) { childProcessor.handleEvent(childEvent) }
            coVerify(exactly = 1) { childProcessor.handleEvent(otherChildEvent) }
            entitiesRepository.get(childEvent.arenaTable, childEvent.arenaId)?.status shouldBe Handled
            entitiesRepository.get(otherChildEvent.arenaTable, otherChildEvent.arenaId)?.status shouldBe Handled
            events.get(parentEvent.arenaTable, parentEvent.arenaId)?.status shouldBe ProcessingStatus.Processed
        }

        test("should allow retry when dependency lookup fails after saving the parent") {
            persistEvent(parentEvent, mappingStatus = Ignored)
            persistEvent(childEvent, mappingStatus = Ignored)
            val parentProcessor = createParentProcessor(getDependents = { error("Avbrutt avhengighetsoppslag") })
            val childProcessor = createChildProcessor()
            val failingService = createService(listOf(parentProcessor, childProcessor))

            failingService.processEvent(parentEvent)

            entitiesRepository.get(parentEvent.arenaTable, parentEvent.arenaId)?.status shouldBe Handled
            entitiesRepository.get(childEvent.arenaTable, childEvent.arenaId)?.status shouldBe Ignored
            events.get(parentEvent.arenaTable, parentEvent.arenaId)?.status shouldBe ProcessingStatus.Failed
            coVerify(exactly = 0) { childProcessor.handleEvent(any()) }

            createService(listOf(createParentProcessor(), childProcessor)).retryEvents(
                table = parentEvent.arenaTable,
                status = ProcessingStatus.Failed
            )

            entitiesRepository.get(childEvent.arenaTable, childEvent.arenaId)?.status shouldBe Handled
            events.get(parentEvent.arenaTable, parentEvent.arenaId)?.status shouldBe ProcessingStatus.Processed
        }

        test("should reconsider ignored dependent events on the next parent event") {
            persistEvent(parentEvent, mappingStatus = Handled)
            persistEvent(childEvent, mappingStatus = Ignored)
            val ignoredChildProcessor = spyk(
                ArenaEventTestProcessor(eventIsRelevant = { it.arenaTable == childEvent.arenaTable }) {
                    ProcessingResult(Ignored).right()
                },
            )

            createService(listOf(createParentProcessor(), ignoredChildProcessor)).processEvent(parentEvent)

            coVerify(exactly = 1) { ignoredChildProcessor.handleEvent(childEvent) }
            entitiesRepository.get(childEvent.arenaTable, childEvent.arenaId)?.status shouldBe Ignored
            events.get(childEvent.arenaTable, childEvent.arenaId)?.status shouldBe ProcessingStatus.Processed
            events.get(parentEvent.arenaTable, parentEvent.arenaId)?.status shouldBe ProcessingStatus.Processed

            createService(listOf(createParentProcessor(), createChildProcessor())).processEvent(parentEvent)

            entitiesRepository.get(childEvent.arenaTable, childEvent.arenaId)?.status shouldBe Handled
        }

        test("should retry a failed dependent event independently") {
            persistEvent(parentEvent, mappingStatus = Handled)
            persistEvent(childEvent, mappingStatus = Ignored)
            val failingChildProcessor = ArenaEventTestProcessor(
                eventIsRelevant = { it.arenaTable == childEvent.arenaTable },
            ) {
                ProcessingError.ProcessingFailed("Levering feilet").left()
            }

            createService(listOf(createParentProcessor(), failingChildProcessor)).processEvent(parentEvent)

            events.get(parentEvent.arenaTable, parentEvent.arenaId)?.status shouldBe ProcessingStatus.Processed
            events.get(childEvent.arenaTable, childEvent.arenaId)?.status shouldBe ProcessingStatus.Failed
            entitiesRepository.get(childEvent.arenaTable, childEvent.arenaId)?.status shouldBe Ignored

            createService(listOf(createParentProcessor(), createChildProcessor())).retryEvents(
                table = childEvent.arenaTable,
                status = ProcessingStatus.Failed
            )

            events.get(childEvent.arenaTable, childEvent.arenaId)?.status shouldBe ProcessingStatus.Processed
            entitiesRepository.get(childEvent.arenaTable, childEvent.arenaId)?.status shouldBe Handled
        }

        test("should not replay ignored dependent events when deleting the parent") {
            persistEvent(parentEvent, mappingStatus = Handled)
            persistEvent(childEvent, mappingStatus = Ignored)
            val childProcessor = createChildProcessor()

            createService(listOf(createParentProcessor(), childProcessor)).processEvent(
                parentEvent.copy(operation = ArenaEvent.Operation.Delete)
            )

            coVerify(exactly = 0) { childProcessor.handleEvent(any()) }
            entitiesRepository.get(childEvent.arenaTable, childEvent.arenaId)?.status shouldBe Ignored
            events.get(parentEvent.arenaTable, parentEvent.arenaId)?.status shouldBe ProcessingStatus.Processed
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
            persistEvent(pendingEvent, mappingStatus = Unhandled)
            persistEvent(processedEvent, mappingStatus = Handled)
            persistEvent(failedEvent, mappingStatus = Unhandled)

            val service = ArenaEventService(events = events, processors = listOf(), entities = entities)
            service.setReplayStatusForEvents(table, Handled)

            events.get(table, pendingEvent.arenaId)?.status shouldBe ProcessingStatus.Pending
            events.get(table, processedEvent.arenaId)?.status shouldBe ProcessingStatus.Replay
            events.get(table, failedEvent.arenaId)?.status shouldBe ProcessingStatus.Failed
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

            events.get(table, pendingEvent.arenaId)?.retries shouldBe 0
            events.get(table, processedEvent.arenaId)?.retries shouldBe 0
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

            events.get(table, pendingEvent.arenaId)?.retries shouldBe 1
            events.get(table, processedEvent.arenaId)?.retries shouldBe 1
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
