package no.nav.mulighetsrommet.arena.adapter.services

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.right
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import no.nav.mulighetsrommet.arena.adapter.events.processors.ArenaEventProcessor
import no.nav.mulighetsrommet.arena.adapter.metrics.Metrics
import no.nav.mulighetsrommet.arena.adapter.metrics.recordSuspend
import no.nav.mulighetsrommet.arena.adapter.models.ProcessingError
import no.nav.mulighetsrommet.arena.adapter.models.ProcessingResult
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTable
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping.Status.*
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.repositories.ArenaEventRepository
import org.slf4j.LoggerFactory
import kotlin.time.measureTime

class ArenaEventService(
    private val config: Config = Config(),
    private val events: ArenaEventRepository,
    private val processors: List<ArenaEventProcessor>,
    private val entities: ArenaEntityService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    data class Config(
        val channelCapacity: Int = 1,
        val numChannelConsumers: Int = 1,
        val maxRetries: Int = 0,
    )

    suspend fun deleteEntities(table: ArenaTable, ids: List<String>) {
        logger.info("Deleting entities from table=$table, ids=$ids")

        ids.forEach { id ->
            events.get(table, id)?.also { event ->
                handleDeleteEntityForEvent(event)
            }
        }
    }

    suspend fun processEvent(event: ArenaEvent) {
        Metrics.processArenaEventTimer(event.arenaTable.table).recordSuspend {
            logger.info("Persisting event: table=${event.arenaTable}, id=${event.arenaId}")
            val eventToProcess = events.upsert(event)

            handleEvent(eventToProcess)
        }
    }

    suspend fun replayEvent(table: ArenaTable, id: String): ArenaEvent? {
        logger.info("Replaying event table=$table, id=$id")

        return events.get(table, id)?.also { event ->
            handleEvent(event)
        }
    }

    suspend fun retryEvents(table: ArenaTable? = null, status: ArenaEvent.ProcessingStatus? = null) {
        logger.info("Retrying events from table=$table, status=$status")

        consumeEvents(table, status, config.maxRetries) { event ->
            Metrics.retryArenaEventTimer(event.arenaTable.table).recordSuspend {
                val eventToRetry = event.copy(retries = event.retries + 1)
                handleEvent(eventToRetry)
            }
        }
    }

    fun setReplayStatusForEvents(table: ArenaTable, status: ArenaEntityMapping.Status) {
        logger.info("Setting replay status to events from table=$table, status=$status")

        events.updateProcessingStatusFromEntityStatus(table, status, ArenaEvent.ProcessingStatus.Replay)
    }

    private suspend fun handleEvent(event: ArenaEvent) {
        try {
            logger.info("Processing event: table=${event.arenaTable}, id=${event.arenaId}")
            val mapping = entities.getOrCreateMapping(event)

            processors
                .filter { it.shouldHandleEvent(event) }
                .fold<ArenaEventProcessor, Either<ProcessingError, ProcessingResult>>(ProcessingResult(Unhandled).right()) { result, processor ->
                    handleEventWithProcessor(result, processor, event, mapping)
                }
                .onRight {
                    entities.upsertMapping(mapping.copy(status = it.status, message = it.message))
                    val status = when (it.status) {
                        Unhandled -> ArenaEvent.ProcessingStatus.Pending
                        else -> ArenaEvent.ProcessingStatus.Processed
                    }
                    events.upsert(event.copy(status = status, message = null))
                }
                .onLeft {
                    logger.info("Failed to process event: table=${event.arenaTable}, id=${event.arenaId}")
                    events.upsert(event.copy(status = it.status, message = it.message))
                }
        } catch (e: Throwable) {
            logger.warn("Failed to process event table=${event.arenaTable}, id=${event.arenaId}", e)

            events.upsert(event.copy(status = ArenaEvent.ProcessingStatus.Failed, message = e.localizedMessage))
        }
    }

    private suspend fun handleEventWithProcessor(
        resultOrError: Either<ProcessingError, ProcessingResult>,
        processor: ArenaEventProcessor,
        event: ArenaEvent,
        mapping: ArenaEntityMapping,
    ) = resultOrError
        .flatMap { result ->
            if (result.status != Ignored) {
                processor.handleEvent(event)
            } else {
                result.right()
            }
        }
        .flatMap { result ->
            if (mapping.status == Handled && result.status == Ignored) {
                logger.info("Sletter entity som tidligere var håndtert men nå skal ignoreres: table=${event.arenaTable}, id=${event.arenaId}, reason=${result.message}")
                deleteEntity(processor, event).map { result }
            } else {
                result.right()
            }
        }

    private suspend fun handleDeleteEntityForEvent(event: ArenaEvent) {
        processors
            .filter { it.shouldHandleEvent(event) }
            .forEach { processor ->
                logger.info("Deleting entity: table=${event.arenaTable}, id=${event.arenaId}")
                deleteEntity(processor, event).onLeft {
                    throw RuntimeException(it.message)
                }
            }
    }

    private suspend fun deleteEntity(processor: ArenaEventProcessor, event: ArenaEvent): Either<ProcessingError, Unit> {
        return processor.deleteEntity(event)
            .onLeft {
                logger.warn("Klarte ikke slette entity: table=${event.arenaTable}, id=${event.arenaId}, status=${it.status}, message=${it.message}")

                if (it is ProcessingError.ForeignKeyViolation) {
                    logger.info("Forsøker å gjenspille avhengigheter til event: table=${event.arenaTable}, id=${event.arenaId}")
                    replayDependentEntities(processor, event)
                }
            }
    }

    private suspend fun replayDependentEntities(
        processor: ArenaEventProcessor,
        event: ArenaEvent,
    ) {
        val dependentEntities = processor.getDependentEntities(event)

        logger.info("Gjenspiller ${dependentEntities.size} avhengigheter til event: table=${event.arenaTable}, id=${event.arenaId}")

        dependentEntities.forEach { mapping ->
            replayEvent(mapping.arenaTable, mapping.arenaId)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun consumeEvents(
        table: ArenaTable?,
        status: ArenaEvent.ProcessingStatus?,
        maxRetries: Int? = null,
        consumer: suspend (ArenaEvent) -> Unit,
    ) = coroutineScope {
        var count = 0
        var prevId: String? = null

        // Produce events in a separate coroutine
        val events = produce(capacity = config.channelCapacity) {
            do {
                val events = events.getAll(
                    table = table,
                    idGreaterThan = prevId,
                    status = status,
                    retriesLessThan = maxRetries,
                    limit = config.channelCapacity,
                )

                events.forEach { send(it) }

                prevId = events.lastOrNull()?.arenaId

                count += events.size
            } while (isActive && events.isNotEmpty())

            close()
        }

        val time = measureTime {
            // Create `numConsumers` coroutines to process the events simultaneously
            (0..config.numChannelConsumers)
                .map {
                    async {
                        events.consumeEach { event ->
                            consumer.invoke(event)
                        }
                    }
                }
                .awaitAll()
        }

        logger.info("Consumed $count events in $time")
    }

    fun getStaleEvents(retriesGreaterThanOrEqual: Int): List<ArenaEvent> {
        return events.getAll(
            retriesGreaterThanOrEqual = retriesGreaterThanOrEqual,
            status = ArenaEvent.ProcessingStatus.Failed,
        )
    }
}
