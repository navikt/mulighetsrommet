package no.nav.mulighetsrommet.arena.adapter.services

import kotlinx.serialization.Serializable
import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.arena.adapter.Database
import org.intellij.lang.annotations.Language

enum class TopicType {
    CONSUMER, PRODUCER
}

class TopicService(private val db: Database) {

    fun getTopics(): List<Topic> {
        val query = """
            select id, key, topic, type, running from topics
        """.trimIndent()
        val queryResult = queryOf(query).map { toTopic(it) }.asList
        return db.session.run(queryResult)
    }

    fun updateRunningStateByTopics(topics: List<Topic>): List<Topic> {
        val updatedTopics = mutableListOf<Topic>()
        @Language("PostgreSQL")
        val query = """
            update topics set running = ? where id = ? and running not like ? returning *
        """.trimIndent()
        db.session.transaction { tx ->
            topics.forEach {
                val updatedTopic = tx.run(queryOf(query, it.running, it.id, it.running).map { toTopic(it) }.asSingle)
                if (updatedTopic != null) {
                    updatedTopics.add(updatedTopic)
                }
            }
        }
    }

    fun upsertConsumerTopics(consumers: List<TopicConsumer>): List<Topic> {
        @Language("PostgreSQL")
        val query = """
           insert into topics (key, topic, type)
            values (?, ?, ?::topic_type)
            on conflict (key)
            do update set
                key = excluded.key,
                topic = excluded.topic
            returning * 
        """.trimIndent()
        db.session.transaction { tx ->
            consumers.forEach {
                tx.run(queryOf(query, it.key, it.topic, TopicType.CONSUMER.toString()).asExecute)
            }
        }
    }

    private fun toTopic(row: Row) = Topic(
        id = row.int("id"),
        key = row.string("key"),
        topic = row.string("topic"),
        type = TopicType.valueOf(row.string("type")),
        running = row.boolean("running")
    )

    @Serializable
    data class Topic(
        val id: Int,
        val key: String,
        val topic: String,
        val type: TopicType,
        val running: Boolean
    )
}
