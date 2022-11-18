package no.nav.mulighetsrommet.arena.adapter.services

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import no.nav.mulighetsrommet.arena.adapter.consumers.ArenaTopicConsumer
import no.nav.mulighetsrommet.arena.adapter.kafka.ConsumerGroup
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.repositories.EventRepository
import org.slf4j.LoggerFactory

class TopicService(
    private val events: EventRepository,
    private val group: ConsumerGroup<ArenaTopicConsumer>,
    private val config: Config = Config(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    data class Config(
        val channelCapacity: Int = 1,
        val numChannelConsumers: Int = 1,
    )

    suspend fun replayEvent(id: Int) = coroutineScope {
        logger.info("Replaying event id=$id")

        return@coroutineScope events.get(id)?.also { event ->
            val relevantConsumers = group.consumers.filter { it.config.topic == event.topic }
            replay(relevantConsumers, event)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun replayEvents(topic: String, id: Int? = null) = coroutineScope {
        logger.info("Replaying events from topic=$topic, id=$id")

        // Produce events in a separate coroutine
        val events = produce(capacity = config.channelCapacity) {
            var prevEventId: Int? = id
            do {
                events.getAll(topic, limit = config.channelCapacity, id = prevEventId)
                    .also { prevEventId = it.lastOrNull()?.id }
                    .forEach { send(it) }
            } while (isActive && prevEventId != null)

            logger.info("All events produced, closing channel...")
            close()
        }

        val relevantConsumers = group.consumers.filter { it.config.topic == topic }

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
                logger.warn("Failed to replay event ${event.id}", it)
                throw it
            }
        }
    }
}
