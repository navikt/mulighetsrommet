package no.nav.mulighetsrommet.arena.adapter.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.Database
import org.intellij.lang.annotations.Language
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class EventRepository(private val db: Database) {
    private val logger: Logger = LoggerFactory.getLogger(EventRepository::class.java)

    fun saveEvent(topic: String, key: String, payload: String) {
        @Language("PostgreSQL")
        val query = """
            insert into events(topic, key, payload)
            values (?, ?, ?::jsonb)
            on conflict (topic, key)
            do update set
                payload = excluded.payload
        """.trimIndent()

        queryOf(query, topic, key, payload)
            .asUpdate
            .let { db.run(it) }
    }

    fun getEvents(topic: String, limit: Int, id: Int? = null): List<Event> {
        logger.info("Getting events topic=$topic, amount=$limit, id=$id")

        @Language("PostgreSQL")
        val query = """
            select id, payload
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
        payload = string("payload"),
    )
}

data class Event(
    val id: Int,
    val payload: String,
)
