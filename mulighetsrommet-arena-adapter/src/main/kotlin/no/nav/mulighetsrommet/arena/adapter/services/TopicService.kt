package no.nav.mulighetsrommet.arena.adapter.services

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.arena.adapter.kafka.ConsumerGroup
import no.nav.mulighetsrommet.arena.adapter.repositories.EventRepository
import org.slf4j.LoggerFactory

class TopicService(
    private val events: EventRepository,
    private val group: ConsumerGroup,
    private val config: Config = Config(),
) {
    private val logger = LoggerFactory.getLogger(TopicService::class.java)

    data class Config(
        val channelCapacity: Int = 1,
        val numChannelConsumers: Int = 1,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun replayEvents(topic: String, id: Int? = null) = coroutineScope {
        logger.info("Replaying events from topic '{}'", topic)

        // Produce events in a separate coroutine
        val events = produce(capacity = config.channelCapacity) {
            var prevEventId: Int? = id
            do {
                events.getEvents(topic, limit = config.channelCapacity, id = prevEventId)
                    .also { prevEventId = it.lastOrNull()?.id }
                    .forEach { send(it) }
            } while (isActive && prevEventId != null)

            logger.info("All events produced, closing channel...")
            close()
        }

        val relevantConsumers = group.consumers.filter { it.consumerConfig.topic == topic }

        // Create `numConsumers` coroutines to process the events simultaneously
        (0..config.numChannelConsumers)
            .map {
                async {
                    events.consumeEach { event ->
                        val payload = Json.parseToJsonElement(event.payload)
                        relevantConsumers.forEach { consumer ->
                            runCatching {
                                consumer.replayEvent(payload)
                            }.onFailure {
                                logger.warn("Failed to replay event ${event.id}: ${it.stackTraceToString()}")
                                throw it
                            }
                        }
                    }
                }
            }
            .awaitAll()
    }
}
