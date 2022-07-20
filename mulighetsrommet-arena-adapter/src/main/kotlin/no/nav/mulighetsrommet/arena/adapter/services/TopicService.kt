package no.nav.mulighetsrommet.arena.adapter.services

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlinx.serialization.json.Json
import kotliquery.Row
import no.nav.mulighetsrommet.arena.adapter.kafka.TopicConsumer
import no.nav.mulighetsrommet.arena.adapter.repositories.Event
import no.nav.mulighetsrommet.arena.adapter.repositories.EventRepository
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class TopicService(
    private val events: EventRepository,
    private val consumers: List<TopicConsumer<*>>,
    private val channelCapacity: Int = 200,
    private val numChannelConsumers: Int = 20
) {
    private val logger = LoggerFactory.getLogger(TopicService::class.java)

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun replayEvents(topic: String, since: LocalDateTime? = null) = coroutineScope {
        logger.info("Replaying events from topic '{}'", topic)

        // Produce events in a separate coroutine
        val events = produce(capacity = channelCapacity) {
            var prevEventTime: LocalDateTime? = since
            do {
                events.getEvents(topic, amount = channelCapacity, createdAfter = prevEventTime)
                    .also { prevEventTime = it.lastOrNull()?.createdAt }
                    .forEach {
                        logger.info("Sending event {}", it.id)
                        send(it)
                    }
            } while (isActive && prevEventTime != null)

            logger.info("All events produced, closing channel...")
            close()
        }

        val relevantConsumers = consumers.filter { it.topic == topic }

        // Create `numConsumers` coroutines to process the events simultaneously
        (0..numChannelConsumers)
            .map {
                async {
                    events.consumeEach { event ->
                        val payload = Json.parseToJsonElement(event.payload)
                        relevantConsumers.forEach { consumer ->
                            consumer.replayEvent(payload)
                        }
                    }
                }
            }
            .awaitAll()
    }
}

private fun Row.toTopic(): String {
    return string("topic")
}

private fun Row.toEvent(): Event {
    return Event(
        id = int("id"),
        payload = string("payload"),
        createdAt = localDateTime("created_at")
    )
}
