package no.nav.mulighetsrommet.arena.adapter.services

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.arena.adapter.Database
import no.nav.mulighetsrommet.arena.adapter.kafka.TopicConsumer
import org.intellij.lang.annotations.Language
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime

class TopicService(
    private val db: Database,
    private val consumers: List<TopicConsumer<*>>,
    private val capacity: Int = 200,
    private val numConsumers: Int = 20,
) {
    private val logger: Logger = LoggerFactory.getLogger(TopicService::class.java)

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

    suspend fun replayEvents(topic: String, since: LocalDateTime?) = coroutineScope {
        logger.info("Replaying events from topic '{}'", topic)

        val relevantConsumers = consumers.filter { it.topic == topic }

        val events = produce(capacity = capacity) {
            var prevEventTime: LocalDateTime? = since
            do {
                getEvents(topic, amount = capacity, since = prevEventTime)
                    .also { prevEventTime = it.lastOrNull()?.createdAt }
                    .forEach {
                        logger.info("Sending event {}", it.id)
                        send(it)
                    }
            } while (isActive && prevEventTime != null)

            logger.info("All events produced, closing channel...")
            close()
        }

        (0..numConsumers)
            .map {
                async {
                    events.consumeEach { event ->
                        val payload = Json.parseToJsonElement(event.payload)
                        relevantConsumers.forEach { consumer ->
                            consumer.replayEvent(payload)
                        }
                    }
                }
            }
            .awaitAll()
    }

    private fun getEvents(topic: String, amount: Int, since: LocalDateTime?): List<Event> {
        logger.info("Getting events topic={}, amount={}, since={}", topic, amount, since)

        @Language("PostgreSQL")
        val query = """
            select id, payload, created_at
            from events
            where topic = :topic
            and created_at > :since
            order by created_at
            limit :amount
        """.trimIndent()

        return queryOf(
            query,
            mapOf(
                "topic" to topic,
                "amount" to amount,
                "since" to (since ?: LocalDate.parse("1900-01-01"))
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
