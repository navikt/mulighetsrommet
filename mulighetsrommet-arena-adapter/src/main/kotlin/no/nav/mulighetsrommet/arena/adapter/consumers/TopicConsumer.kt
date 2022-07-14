package no.nav.mulighetsrommet.arena.adapter.consumers

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import no.nav.mulighetsrommet.arena.adapter.Database
import org.apache.kafka.clients.consumer.ConsumerRecord

abstract class TopicConsumer<T>(private val db: Database) {
    abstract val topic: String

    fun processEvent(event: ConsumerRecord<String, String>) {
        val eventPayload = Json.parseToJsonElement(event.value())
        val decodedPayload = toDomain(eventPayload)
        if (shouldProcessEvent(decodedPayload)) {
            val key = resolveKey(decodedPayload)
            db.persistKafkaEvent(
                event.topic(),
                key,
                event.value()
            )

            handleEvent(decodedPayload)
        }
    }

    protected abstract fun toDomain(payload: JsonElement): T

    protected open fun shouldProcessEvent(payload: T): Boolean = true

    protected abstract fun resolveKey(payload: T): String

    protected abstract fun handleEvent(payload: T)
}
