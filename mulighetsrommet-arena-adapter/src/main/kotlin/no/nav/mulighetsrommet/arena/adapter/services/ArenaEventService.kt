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
            replay(group.consumers, data)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun replayEvents(table: String? = null) = coroutineScope {
        logger.info("Replaying events from table=$table")

        // Produce events in a separate coroutine
        val events = produce(capacity = config.channelCapacity) {
            var offset = 0
            do {
                val events = arenaEvents.getAll(table, limit = config.channelCapacity, offset = offset)

                events.forEach { send(it) }

                offset += events.size
            } while (isActive && events.isNotEmpty())

            logger.info("All events produced, closing channel...")
            close()
        }

        // Create `numConsumers` coroutines to process the events simultaneously
        (0..config.numChannelConsumers)
            .map {
                async {
                    events.consumeEach { event ->
                        replay(group.consumers, event)
                    }
                }
            }
            .awaitAll()
    }

    private suspend fun replay(consumers: List<ArenaTopicConsumer>, event: ArenaEvent) {
        consumers
            .filter { it.arenaTable == event.arenaTable }
            .forEach { consumer ->
                runCatching {
                    consumer.replayEvent(event)
                }.onFailure {
                    logger.warn("Failed to replay event ${event.arenaId}", it)
                    throw it
                }
            }
    }
}
