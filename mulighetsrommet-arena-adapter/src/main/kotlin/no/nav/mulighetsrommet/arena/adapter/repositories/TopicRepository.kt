package no.nav.mulighetsrommet.arena.adapter.repositories

import kotlinx.serialization.Serializable
import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.arena.adapter.kafka.TopicConsumer
import no.nav.mulighetsrommet.database.Database
import org.intellij.lang.annotations.Language

enum class TopicType {
    CONSUMER, PRODUCER
}

class TopicRepository(private val db: Database) {

    fun selectAll(): List<Topic> {
        val query = """
            select id, key, topic, type, running from topics order by id
        """.trimIndent()
        val queryResult = queryOf(query).map { it.toTopic() }.asList
        return db.run(queryResult)
    }

    fun updateRunning(topics: List<Topic>) {
        @Language("PostgreSQL")
        val query = """
            update topics set running = ? where id = ?
        """.trimIndent()
        db.transaction { tx ->
            topics.forEach {
                tx.run(queryOf(query, it.running, it.id).asExecute)
            }
        }
    }

    fun upsertTopics(consumers: List<TopicConsumer<*>>) {
        @Language("PostgreSQL")
        val query = """
           insert into topics (key, topic, type)
            values (?, ?, ?::topic_type)
            on conflict (key)
            do update set
                key = excluded.key,
                topic = excluded.topic
        """.trimIndent()
        db.transaction { tx ->
            consumers.forEach {
                tx.run(queryOf(query, it.consumerConfig.key, it.consumerConfig.topic, TopicType.CONSUMER.toString()).asExecute)
            }
        }
    }

    private fun Row.toTopic() = Topic(
        id = int("id"),
        key = string("key"),
        topic = string("topic"),
        type = TopicType.valueOf(string("type")),
        running = boolean("running")
    )
}

@Serializable
data class Topic(
    val id: Int,
    val key: String,
    val topic: String,
    val type: TopicType,
    val running: Boolean
)
