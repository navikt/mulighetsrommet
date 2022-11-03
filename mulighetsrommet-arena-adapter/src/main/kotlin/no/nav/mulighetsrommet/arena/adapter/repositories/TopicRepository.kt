package no.nav.mulighetsrommet.arena.adapter.repositories

import kotlinx.serialization.Serializable
import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.Database
import org.intellij.lang.annotations.Language

enum class TopicType {
    CONSUMER, PRODUCER
}

class TopicRepository(private val db: Database) {

    fun selectAll(): List<Topic> {
        @Language("PostgreSQL")
        val query = """
            select id, topic, type, running from topics order by id
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

    fun upsertTopics(topics: List<Topic>) {
        @Language("PostgreSQL")
        val query = """
           insert into topics (id, topic, type, running)
            values (:id, :topic, :type::topic_type, :running)
            on conflict (id)
            do update set
                topic = excluded.topic,
                type = excluded.type,
                running = excluded.running
        """.trimIndent()
        db.transaction { tx ->
            topics.forEach { topic ->
                queryOf(query, topic.toSqlParameters())
                    .asExecute
                    .let { tx.run(it) }
            }
        }
    }

    private fun Topic.toSqlParameters() = mapOf(
        "id" to id,
        "topic" to topic,
        "type" to type.toString(),
        "running" to running,
    )

    private fun Row.toTopic() = Topic(
        id = string("id"),
        topic = string("topic"),
        type = TopicType.valueOf(string("type")),
        running = boolean("running")
    )
}

@Serializable
data class Topic(
    val id: String,
    val topic: String,
    val type: TopicType,
    val running: Boolean
)
