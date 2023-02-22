package no.nav.mulighetsrommet.arena.adapter.services

import arrow.core.getOrHandle
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import no.nav.mulighetsrommet.arena.adapter.events.processors.ArenaEventProcessor
import no.nav.mulighetsrommet.arena.adapter.kafka.ConsumerGroup
import no.nav.mulighetsrommet.arena.adapter.metrics.Metrics
import no.nav.mulighetsrommet.arena.adapter.metrics.recordSuspend
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTable
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.repositories.ArenaEventRepository
import org.slf4j.LoggerFactory
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

class ArenaEventService(
    private val config: Config = Config(),
    private val events: ArenaEventRepository,
    private val group: ConsumerGroup<ArenaEventProcessor>
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    data class Config(
        val channelCapacity: Int = 1,
        val numChannelConsumers: Int = 1,
        val maxRetries: Int = 0
    )

    suspend fun deleteEntities(table: ArenaTable, ids: List<String>) {
        logger.info("Deleting entities from table=$table, ids=$ids")

        ids.forEach { id ->
            events.get(table, id)?.also {
                deleteEntity(it)
            }
        }
    }

    suspend fun replayEvent(table: ArenaTable, id: String): ArenaEvent? {
        logger.info("Replaying event table=$table, id=$id")

        return events.get(table, id)?.also { event ->
            processEvent(event)
        }
    }

    suspend fun retryEvents(table: ArenaTable? = null, status: ArenaEvent.ProcessingStatus? = null) {
        logger.info("Retrying events from table=$table, status=$status")

        consumeEvents(table, status, config.maxRetries) { event ->
            Metrics.retryArenaEventTimer(event.arenaTable.table).recordSuspend {
                val eventToRetry = event.copy(retries = event.retries + 1)
                processEvent(eventToRetry)
            }
        }
    }

    fun setReplayStatusForEvents(table: ArenaTable, status: ArenaEvent.ProcessingStatus? = null) {
        logger.info("Setting replay status to events from table=$table, status=$status")

        events.updateStatus(table, status, ArenaEvent.ProcessingStatus.Replay)
    }

    private suspend fun processEvent(event: ArenaEvent) {
        group.consumers
            .filter { it.arenaTable == event.arenaTable }
            .forEach { consumer ->
                runCatching {
                    logger.info("Processing event: table=${event.arenaTable}, id=${event.arenaId}")

                    val (status, message) = consumer.handleEvent(event)
                        .map { Pair(it, null) }
                        .getOrHandle {
                            logger.info("Event processing ended with an error: table=${event.arenaTable}, id=${event.arenaId}, status=${it.status}, message=${it.message}")
                            Pair(it.status, it.message)
                        }

                    events.upsert(event.copy(status = status, message = message))
                }.onFailure {
                    logger.warn("Failed to process event table=${event.arenaTable}, id=${event.arenaId}", it)
                    events.upsert(
                        event.copy(
                            status = ArenaEvent.ProcessingStatus.Failed,
                            message = it.localizedMessage
                        )
                    )
                }
            }
    }

    private suspend fun deleteEntity(event: ArenaEvent) {
        group.consumers
            .filter { it.arenaTable == event.arenaTable }
            .forEach { consumer ->

                logger.info("Deleting entity: table=${event.arenaTable}, id=${event.arenaId}")

                consumer.deleteEntity(event).tapLeft {
                    throw RuntimeException(it.message)
                }
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
    private suspend fun consumeEvents(
        table: ArenaTable?,
        status: ArenaEvent.ProcessingStatus?,
        maxRetries: Int? = null,
        consumer: suspend (ArenaEvent) -> Unit
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
                    maxRetries = maxRetries,
                    limit = config.channelCapacity
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
}
