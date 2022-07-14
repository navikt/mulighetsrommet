package no.nav.mulighetsrommet.arena.adapter.consumers

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotliquery.queryOf
import no.nav.mulighetsrommet.arena.adapter.Database
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.intellij.lang.annotations.Language
import org.slf4j.Logger

abstract class TopicConsumer<T>(private val db: Database) {
    abstract val logger: Logger
    abstract val topic: String

    fun processEvent(event: ConsumerRecord<String, String>) {
        val eventPayload = Json.parseToJsonElement(event.value())
        val decodedPayload = toDomain(eventPayload)
        if (shouldProcessEvent(decodedPayload)) {
            val key = resolveKey(decodedPayload)

            logger.debug("Persisting event: topic=$topic, key=$key")
            persistEvent(
                event.topic(),
                key,
                event.value()
            )

            logger.debug("Handling event: topic=$topic, key=$key")
            handleEvent(decodedPayload)
        }
    }

    protected abstract fun toDomain(payload: JsonElement): T

    protected open fun shouldProcessEvent(payload: T): Boolean = true

    protected abstract fun resolveKey(payload: T): String

    protected abstract fun handleEvent(payload: T)

    private fun persistEvent(topic: String, key: String, payload: String) {
        @Language("PostgreSQL")
        val query = """
            insert into events(topic, key, payload)
            values (?, ?, ?::jsonb)
            on conflict (topic, key)
            do update set
                payload = excluded.payload
        """.trimIndent()

        db.session.run(queryOf(query, topic, key, payload).asUpdate)
    }
}
