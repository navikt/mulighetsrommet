package no.nav.mulighetsrommet.arena.adapter.services

import arrow.core.getOrHandle
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import no.nav.mulighetsrommet.arena.adapter.consumers.ArenaTopicConsumer
import no.nav.mulighetsrommet.arena.adapter.kafka.ConsumerGroup
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.repositories.ArenaEventRepository
import org.slf4j.LoggerFactory

class ArenaEventService(
    private val config: Config = Config(),
    private val events: ArenaEventRepository,
    private val group: ConsumerGroup<ArenaTopicConsumer>,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    data class Config(
        val channelCapacity: Int = 1,
        val numChannelConsumers: Int = 1,
        val maxRetries: Int = 0,
    )

    suspend fun replayEvent(table: String, id: String): ArenaEvent? = coroutineScope {
        logger.info("Replaying event table=$table, id=$id")

        events.get(table, id)?.also { event ->
            processEvent(group.consumers, event)
        }
    }

    suspend fun replayEvents(table: String? = null, status: ArenaEvent.ConsumptionStatus? = null) = coroutineScope {
        logger.info("Replaying events from table=$table")

        consumeEvents(table, status) { event ->
            processEvent(group.consumers, event)
        }
    }

    suspend fun retryEvents(table: String? = null, status: ArenaEvent.ConsumptionStatus? = null) = coroutineScope {
        logger.info("Retrying events from table=$table")

        consumeEvents(table, status, config.maxRetries) { event ->
            val eventToRetry = event.copy(retries = event.retries + 1)
            processEvent(group.consumers, eventToRetry)
        }
    }

    private suspend fun processEvent(consumers: List<ArenaTopicConsumer>, event: ArenaEvent) {
        consumers
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
                    logger.warn("Failed to process event table=${event.arenaTable}, id=${event.arenaId}:", it)
                    throw it
                }
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun consumeEvents(
        table: String?,
        status: ArenaEvent.ConsumptionStatus?,
        maxRetries: Int? = null,
        consumer: suspend (ArenaEvent) -> Unit
    ) = coroutineScope {
        var offset = 0

        // Produce events in a separate coroutine
        val events = produce(capacity = config.channelCapacity) {
            do {
                val events =
                    events.getAll(
                        table = table,
                        status = status,
                        maxRetries = maxRetries,
                        limit = config.channelCapacity,
                        offset = offset
                    )

                events.forEach { send(it) }

                offset += events.size
            } while (isActive && events.isNotEmpty())

            logger.info("Produced $offset events")
            close()
        }

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

        logger.info("Consumed $offset events")
    }
}
