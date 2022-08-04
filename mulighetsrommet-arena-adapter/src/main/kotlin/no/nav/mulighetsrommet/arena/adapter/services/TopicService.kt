package no.nav.mulighetsrommet.arena.adapter.services

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.arena.adapter.kafka.ConsumerSetup
import no.nav.mulighetsrommet.arena.adapter.repositories.EventRepository
import org.slf4j.LoggerFactory

class TopicService(
    private val events: EventRepository,
    private val consumerSetup: ConsumerSetup,
    private val channelCapacity: Int = 200,
    private val numChannelConsumers: Int = 20
) {
    private val logger = LoggerFactory.getLogger(TopicService::class.java)

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun replayEvents(topic: String, id: Int? = null) = coroutineScope {
        logger.info("Replaying events from topic '{}'", topic)

        // Produce events in a separate coroutine
        val events = produce(capacity = channelCapacity) {
            var prevEventId: Int? = id
            do {
                events.getEvents(topic, limit = channelCapacity, id = prevEventId)
                    .also { prevEventId = it.lastOrNull()?.id }
                    .forEach { send(it) }
            } while (isActive && prevEventId != null)

            logger.info("All events produced, closing channel...")
            close()
        }

        val relevantConsumers = consumerSetup.consumers.filter { it.consumerConfig.topic == topic }

        // Create `numConsumers` coroutines to process the events simultaneously
        (0..numChannelConsumers)
            .map {
                async {
                    events.consumeEach { event ->
                        val payload = Json.parseToJsonElement(event.payload)
                        relevantConsumers.forEach { consumer ->
                            runCatching {
                                consumer.replayEvent(payload)
                            }.onFailure {
                                logger.warn("Failed to replay event ${event.id}")
                                throw it
                            }
                        }
                    }
                }
            }
            .awaitAll()
    }
}
