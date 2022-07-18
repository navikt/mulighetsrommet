package no.nav.mulighetsrommet.arena.adapter.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.arena.adapter.Database
import org.intellij.lang.annotations.Language
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime

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

    fun getTopics(): List<String> {
        @Language("PostgreSQL")
        val query = """
            select topic from events group by topic
        """.trimIndent()

        return queryOf(query)
            .map { it.toTopic() }
            .asList
            .let { db.run(it) }
    }

    fun getEvents(topic: String, amount: Int, createdAfter: LocalDateTime? = null): List<Event> {
        logger.info("Getting events topic=$topic, amount=$amount, since=$createdAfter")

        @Language("PostgreSQL")
        val query = """
            select id, payload, created_at
            from events
            where topic = :topic
            and created_at > :created_after
            order by created_at
            limit :amount
        """.trimIndent()

        return queryOf(
            query,
            mapOf(
                "topic" to topic,
                "amount" to amount,
                "created_after" to (createdAfter ?: LocalDate.parse("1900-01-01"))
            )
        )
            .map { it.toEvent() }
            .asList
            .let { db.run(it) }
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

data class Event(
    val id: Int,
    val payload: String,
    val createdAt: LocalDateTime
)
