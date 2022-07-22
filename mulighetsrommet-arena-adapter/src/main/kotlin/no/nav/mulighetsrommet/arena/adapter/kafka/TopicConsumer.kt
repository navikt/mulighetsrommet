package no.nav.mulighetsrommet.arena.adapter.kafka

import kotlinx.serialization.json.JsonElement
import no.nav.mulighetsrommet.arena.adapter.repositories.EventRepository
import org.slf4j.Logger

abstract class TopicConsumer<T>() {
    abstract val logger: Logger

    abstract val topic: String
    abstract val events: EventRepository

    suspend fun processEvent(payload: JsonElement) {
        val parsedPayload = toDomain(payload)
        if (shouldProcessEvent(parsedPayload)) {
            val key = resolveKey(parsedPayload)

            logger.debug("Persisting event: topic=$topic, key=$key")
            events.saveEvent(topic, key, payload.toString())

            logger.debug("Handling event: topic=$topic, key=$key")
            handleEvent(parsedPayload)
        }
    }

    suspend fun replayEvent(payload: JsonElement) {
        val parsedEvent = toDomain(payload)
        if (shouldProcessEvent(parsedEvent)) {
            val key = resolveKey(parsedEvent)

            logger.debug("Replaying event: topic=$topic, key=$key")
            handleEvent(parsedEvent)
        }
    }

    protected abstract fun toDomain(payload: JsonElement): T

    protected open fun shouldProcessEvent(payload: T): Boolean = true

    protected abstract fun resolveKey(payload: T): String

    protected abstract suspend fun handleEvent(payload: T)
}
