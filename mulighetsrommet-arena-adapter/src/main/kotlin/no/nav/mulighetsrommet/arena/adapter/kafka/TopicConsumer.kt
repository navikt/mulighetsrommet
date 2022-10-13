package no.nav.mulighetsrommet.arena.adapter.kafka

import kotlinx.serialization.json.JsonElement
import no.nav.mulighetsrommet.arena.adapter.ConsumerConfig
import no.nav.mulighetsrommet.arena.adapter.repositories.EventRepository
import org.slf4j.Logger

abstract class TopicConsumer<T> {
    abstract val consumerConfig: ConsumerConfig
    abstract val logger: Logger
    abstract val events: EventRepository

    suspend fun processEvent(payload: JsonElement) {
        val parsedPayload = decodeEvent(payload)
        if (shouldProcessEvent(parsedPayload)) {
            val key = resolveKey(parsedPayload)

            logger.info("Persisting event: topic=${consumerConfig.topic}, key=$key")
            events.upsert(consumerConfig.topic, key, payload.toString())

            logger.info("Handling event: topic=${consumerConfig.topic}, key=$key")
            handleEvent(parsedPayload)
        }
    }

    suspend fun replayEvent(payload: JsonElement) {
        val parsedEvent = decodeEvent(payload)
        if (shouldProcessEvent(parsedEvent)) {
            val key = resolveKey(parsedEvent)

            logger.info("Replaying event: topic=${consumerConfig.topic}, key=$key")
            handleEvent(parsedEvent)
        }
    }

    protected abstract fun decodeEvent(payload: JsonElement): T

    protected open fun shouldProcessEvent(event: T): Boolean = true

    protected abstract fun resolveKey(event: T): String

    protected abstract suspend fun handleEvent(event: T)
}
