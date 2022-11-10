package no.nav.mulighetsrommet.arena.adapter.consumers

import arrow.core.Either
import arrow.core.getOrHandle
import kotlinx.serialization.json.JsonElement
import no.nav.mulighetsrommet.arena.adapter.kafka.TopicConsumer
import no.nav.mulighetsrommet.arena.adapter.models.ConsumptionError
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.repositories.EventRepository
import org.slf4j.Logger

abstract class ArenaTopicConsumer : TopicConsumer() {
    abstract val logger: Logger
    abstract val events: EventRepository

    override suspend fun run(event: JsonElement) {
        val data = decodeArenaData(event)
        processEvent(data)
    }

    suspend fun processEvent(e: ArenaEvent): ArenaEvent {
        logger.info("Persisting event: topic=${config.topic}, arena_id=${e.key}")
        val event = events.upsert(e)

        logger.info("Handling event: topic=${config.topic}, key=${event.key}")
        val (status, message) = handleEvent(event)
            .map { Pair(ArenaEvent.ConsumptionStatus.Processed, null) }
            .getOrHandle {
                logger.info("Event consumption failed: ${it.message}")
                Pair(it.status, it.message)
            }

        return events.upsert(event.copy(status = status, message = message))
    }

    suspend fun replayEvent(event: ArenaEvent): ArenaEvent {
        logger.info("Replaying event: topic=${config.topic}, key=${event.key}")
        val (status, message) = handleEvent(event)
            .map { Pair(ArenaEvent.ConsumptionStatus.Processed, null) }
            .getOrHandle {
                logger.info("Failed to replay event: ${it.message}")
                Pair(it.status, it.message)
            }

        return events.upsert(event.copy(status = status, message = message))
    }

    abstract fun decodeArenaData(payload: JsonElement): ArenaEvent

    abstract suspend fun handleEvent(event: ArenaEvent): Either<ConsumptionError, Unit>
}
