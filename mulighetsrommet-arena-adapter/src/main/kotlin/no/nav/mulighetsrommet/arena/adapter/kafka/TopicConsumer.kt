package no.nav.mulighetsrommet.arena.adapter.kafka

import kotlinx.serialization.json.JsonElement
import kotliquery.queryOf
import no.nav.mulighetsrommet.arena.adapter.Database
import org.intellij.lang.annotations.Language
import org.slf4j.Logger

abstract class TopicConsumer<T>(private val db: Database) {
    abstract val logger: Logger
    abstract val topic: String

    fun processEvent(payload: JsonElement) {
        val parsedPayload = toDomain(payload)
        if (shouldProcessEvent(parsedPayload)) {
            val key = resolveKey(parsedPayload)

            logger.debug("Persisting event: topic=$topic, key=$key")
            persistEvent(topic, key, payload.toString())

            logger.debug("Handling event: topic=$topic, key=$key")
            handleEvent(parsedPayload)
        }
    }

    fun replayEvent(payload: JsonElement) {
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

        db.run(queryOf(query, topic, key, payload).asUpdate)
    }
}
