package no.nav.mulighetsrommet.kafka

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.createArrayOfValue
import org.intellij.lang.annotations.Language

enum class TopicType {
    CONSUMER,
    PRODUCER,
}

class TopicRepository(private val db: Database) {

    fun getAll(): List<Topic> = db.session { session ->
        @Language("PostgreSQL")
        val query = """
            select id, topic, type, running from topics order by id
        """.trimIndent()
        return session.list(queryOf(query)) { it.toTopic() }
    }

    fun updateRunning(topics: List<Topic>) = db.transaction { tx ->
        @Language("PostgreSQL")
        val query = """
            update topics set running = ? where id = ?
        """.trimIndent()
        topics.forEach {
            tx.run(queryOf(query, it.running, it.id).asExecute)
        }
    }

    fun setAll(topics: List<Topic>) = db.transaction { tx ->
        @Language("PostgreSQL")
        val upsert = """
            insert into topics (id, topic, type, running)
            values (:id, :topic, :type::topic_type, :running)
            on conflict (id)
            do update set
                topic = excluded.topic,
                type = excluded.type,
                running = excluded.running
        """.trimIndent()

        @Language("PostgreSQL")
        val delete = """
            delete from topics where not(id = any(?))
        """.trimIndent()

        topics.forEach { topic ->
            tx.execute(queryOf(upsert, topic.toSqlParameters()))
        }

        val ids = tx.createArrayOfValue(topics) { it.id }
        tx.execute(queryOf(delete, ids))
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
        running = boolean("running"),
    )
}
