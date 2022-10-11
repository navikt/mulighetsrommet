package no.nav.mulighetsrommet.arena.adapter.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.Database
import org.intellij.lang.annotations.Language
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class EventRepository(private val db: Database) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun upsert(topic: String, key: String, payload: String) {
        @Language("PostgreSQL")
        val query = """
            insert into events(consumption_status, topic, key, payload)
            values (?::consumption_status, ?, ?, ?::jsonb)
            on conflict (topic, key)
            do update set
                payload = excluded.payload
        """.trimIndent()

        queryOf(query, Event.ConsumptionStatus.Processed.toString(), topic, key, payload)
            .asUpdate
            .let { db.run(it) }
    }

    fun get(id: Int): Event? {
        logger.info("Getting event id=$id")

        @Language("PostgreSQL")
        val query = """
            select id, consumption_status, topic, payload
            from events
            where id = ?
        """.trimIndent()

        return queryOf(query, id)
            .map { it.toEvent() }
            .asSingle
            .let { db.run(it) }
    }

    fun getAll(topic: String, limit: Int, id: Int? = null): List<Event> {
        logger.info("Getting events topic=$topic, amount=$limit, id=$id")

        @Language("PostgreSQL")
        val query = """
            select id, consumption_status, topic, payload
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
}

private fun Row.toEvent(): Event {
    return Event(
        id = int("id"),
        status = Event.ConsumptionStatus.valueOf(string("consumption_status")),
        topic = string("topic"),
        payload = string("payload"),
    )
}

data class Event(
    val id: Int,
    val status: ConsumptionStatus,
    val topic: String,
    val payload: String,
) {
    enum class ConsumptionStatus {
        /** Event processing is pending and will be started in the next schedule */
        Pending,

        /** Event is being processed */
        Processing,

        /** Event has been processed */
        Processed,

        /** Processing has failed, event processing can be retried */
        Failed,

        /** Event has been ignored */
        Ignored
    }
}
