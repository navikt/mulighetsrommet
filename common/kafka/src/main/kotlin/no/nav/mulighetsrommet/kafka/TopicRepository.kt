package no.nav.mulighetsrommet.kafka

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.Database
import org.intellij.lang.annotations.Language

enum class TopicType {
    CONSUMER,
    PRODUCER,
}

class TopicRepository(private val db: Database) {

    fun getAll(): List<Topic> {
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

    fun setAll(topics: List<Topic>) {
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

        db.transaction { tx ->
            topics.forEach { topic ->
                queryOf(upsert, topic.toSqlParameters())
                    .asExecute
                    .let { tx.run(it) }
            }

            val ids = tx.createArrayOf("text", topics.map { it.id })
            queryOf(delete, ids)
                .asExecute
                .let { tx.run(it) }
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
        running = boolean("running"),
    )
}
