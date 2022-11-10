package no.nav.mulighetsrommet.arena.adapter.repositories

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.database.Database
import org.intellij.lang.annotations.Language
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class EventRepository(private val db: Database) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun upsert(event: ArenaEvent): ArenaEvent {
        @Language("PostgreSQL")
        val query = """
            insert into events(topic, key, payload, consumption_status, message)
            values (:topic, :key, :payload::jsonb, :status::consumption_status, :message)
            on conflict (topic, key)
            do update set
                payload            = excluded.payload,
                consumption_status = excluded.consumption_status,
                message            = excluded.message
            returning *
        """.trimIndent()

        return queryOf(query, event.toParameters())
            .map { it.toEvent() }
            .asSingle
            .let { db.run(it)!! }
    }

    fun get(id: Int): ArenaEvent? {
        logger.info("Getting event id=$id")

        @Language("PostgreSQL")
        val query = """
            select id, topic, key, payload, consumption_status, message
            from events
            where id = ?
        """.trimIndent()

        return queryOf(query, id)
            .map { it.toEvent() }
            .asSingle
            .let { db.run(it) }
    }

    fun getAll(topic: String, limit: Int, id: Int? = null): List<ArenaEvent> {
        logger.info("Getting events topic=$topic, amount=$limit, id=$id")

        @Language("PostgreSQL")
        val query = """
            select id, topic, key, payload, consumption_status, message
            from events
            where topic = :topic
              and id > :id
            order by id
            limit :limit
        """.trimIndent()

        return queryOf(
            query,
            mapOf(
                "topic" to topic,
                "limit" to limit,
                "id" to (id ?: 0),
            )
        )
            .map { it.toEvent() }
            .asList
            .let { db.run(it) }
    }

    private fun ArenaEvent.toParameters() = mapOf(
        "topic" to topic,
        "key" to key,
        "payload" to payload.toString(),
        "status" to status.name,
        "message" to message
    )

    private fun Row.toEvent() = ArenaEvent(
        id = int("id"),
        topic = string("topic"),
        key = string("key"),
        payload = Json.parseToJsonElement(string("payload")),
        status = ArenaEvent.ConsumptionStatus.valueOf(string("consumption_status")),
        message = stringOrNull("message"),
    )
}
