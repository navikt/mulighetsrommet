package no.nav.mulighetsrommet.arena.adapter.services

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import no.nav.mulighetsrommet.arena.adapter.consumers.ArenaTopicConsumer
import no.nav.mulighetsrommet.arena.adapter.kafka.ConsumerGroup
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.repositories.ArenaEventRepository
import org.slf4j.LoggerFactory

class ArenaEventService(
    private val arenaEvents: ArenaEventRepository,
    private val group: ConsumerGroup<ArenaTopicConsumer>,
    private val config: Config = Config(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    data class Config(
        val channelCapacity: Int = 1,
        val numChannelConsumers: Int = 1,
    )

    suspend fun replayEvent(table: String, id: String) = coroutineScope {
        logger.info("Replaying event table=$table, id=$id")

        return@coroutineScope arenaEvents.get(table, id)?.also { data ->
            val relevantConsumers = group.consumers.filter { it.arenaTable == data.arenaTable }
            replay(relevantConsumers, data)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun replayEvents(table: String, id: String? = null) = coroutineScope {
        logger.info("Replaying events from topic=$table, id=$id")

        // Produce events in a separate coroutine
        val events = produce(capacity = config.channelCapacity) {
            var prevEventId: String? = id
            do {
                arenaEvents.getAll(table, limit = config.channelCapacity, id = prevEventId)
                    .also { prevEventId = it.lastOrNull()?.arenaId }
                    .forEach { send(it) }
            } while (isActive && prevEventId != null)

            logger.info("All events produced, closing channel...")
            close()
        }

        val relevantConsumers = group.consumers.filter { it.arenaTable == table }

        // Create `numConsumers` coroutines to process the events simultaneously
        (0..config.numChannelConsumers)
            .map {
                async {
                    events.consumeEach { event ->
                        replay(relevantConsumers, event)
                    }
                }
            }
            .awaitAll()
    }

    private suspend fun replay(relevantConsumers: List<ArenaTopicConsumer>, event: ArenaEvent) {
        relevantConsumers.forEach { consumer ->
            runCatching {
                consumer.replayEvent(event)
            }.onFailure {
                logger.warn("Failed to replay event ${event.arenaId}", it)
                throw it
            }
        }
    }
}
