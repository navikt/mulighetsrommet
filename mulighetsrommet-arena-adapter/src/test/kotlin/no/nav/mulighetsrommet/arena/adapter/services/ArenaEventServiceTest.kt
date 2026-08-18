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
            events.get(table, processedEvent.arenaId).let {
                it?.status shouldBe ProcessingStatus.Processed
                it?.message shouldBe null
            }
            events.get(table, dependentEvent.arenaId).let {
                it?.status shouldBe ProcessingStatus.Processed
                it?.message shouldBe null
            }

            entitiesRepository.get(table, processedEvent.arenaId).let {
                it?.entityId shouldBe processedEventMapping.entityId
                it?.status shouldBe Handled
                it?.message shouldBe null
            }
            entitiesRepository.get(table, dependentEvent.arenaId).let {
                it?.entityId shouldBe dependentEventMapping.entityId
                it?.status shouldBe Handled
                it?.message shouldBe null
            }
        }

        test("should replay dependent events when entity transitions from Ignored to Handled") {
            val gjennomforingEvent = ArenaEvent(
                status = ProcessingStatus.Pending,
                arenaTable = table,
                operation = ArenaEvent.Operation.Insert,
                arenaId = "10",
                payload = JsonObject(mapOf("after" to JsonObject(mapOf("name" to JsonPrimitive("Gjennomforing"))))),
            )
            val deltakerEvent = ArenaEvent(
                status = ProcessingStatus.Pending,
                arenaTable = ArenaTable.AvtaleInfo,
                operation = ArenaEvent.Operation.Insert,
                arenaId = "11",
                payload = JsonObject(mapOf("after" to JsonObject(mapOf("name" to JsonPrimitive("Deltaker"))))),
            )

            val ignoredDeltakerMapping = entities.getOrCreateMapping(deltakerEvent)

            // Simulerer at gjennomføring først er Ignored, deretter Handled
            var gjennomforingHandleCount = 0
            val gjennomforingProcessor = spyk(
                ArenaEventTestProcessor(
                    eventIsRelevant = { it.arenaId == gjennomforingEvent.arenaId && it.arenaTable == gjennomforingEvent.arenaTable },
                    getDependentEntities = { listOf(ignoredDeltakerMapping) },
                ) {
                    gjennomforingHandleCount++
                    if (gjennomforingHandleCount == 1) {
                        ProcessingResult(Ignored, "Gjennomforing ignorert").right()
                    } else {
                        ProcessingResult(Handled).right()
                    }
                },
            )

            // Simulerer at deltaker er Ignored fordi gjennomføring er Ignored, og Handled ved replay
            var deltakerHandleCount = 0
            val deltakerProcessor = spyk(
                ArenaEventTestProcessor(eventIsRelevant = { it.arenaId == deltakerEvent.arenaId && it.arenaTable == deltakerEvent.arenaTable }) {
                    deltakerHandleCount++
                    if (deltakerHandleCount == 1) {
                        ProcessingResult(
                            Ignored,
                            "Deltaker ignorert fordi tilhørende tiltaksgjennomføring også er ignorert",
                        ).right()
                    } else {
                        ProcessingResult(Handled).right()
                    }
                },
            )

            val service = ArenaEventService(
                events = events,
                processors = listOf(gjennomforingProcessor, deltakerProcessor),
                entities = entities,
            )

            // Prosesser gjennomforing første gang => Ignored
            service.processEvent(gjennomforingEvent)
            entitiesRepository.get(gjennomforingEvent.arenaTable, gjennomforingEvent.arenaId)?.status shouldBe Ignored

            // Prosesser deltaker => Ignored fordi gjennomforing er Ignored
            service.processEvent(deltakerEvent)
            entitiesRepository.get(deltakerEvent.arenaTable, deltakerEvent.arenaId)?.status shouldBe Ignored
            coVerify(exactly = 1) { deltakerProcessor.handleEvent(any()) }

            // Prosesser gjennomforing andre gang => Handled (status endres fra Ignored til Handled) og at deltaker ble gjenspilt og er nå Handled
            service.processEvent(gjennomforingEvent)
            entitiesRepository.get(gjennomforingEvent.arenaTable, gjennomforingEvent.arenaId)?.status shouldBe Handled
            entitiesRepository.get(deltakerEvent.arenaTable, deltakerEvent.arenaId)?.status shouldBe Handled
            coVerify(exactly = 2) { deltakerProcessor.handleEvent(any()) }
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
