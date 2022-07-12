package no.nav.mulighetsrommet.arena.adapter.services

import kotlinx.serialization.Serializable
import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.arena.adapter.Database

enum class TopicType {
    CONSUMER, PRODUCER
}

class TopicService(private val db: Database) {

    fun getTopics(): List<Topic> {
        val query = """
            select id, name, topic, type, running from topics
        """.trimIndent()
        val queryResult = queryOf(query).map { toTopic(it) }.asList
        return db.session.run(queryResult)
    }

    fun updateRunningStateByTopics(topics: List<Topic>) {
        db.session.transaction { tx ->
            topics.forEach {
                tx.run(queryOf("update topics set running = ? where id = ?", it.running, it.id).asUpdate)
            }
        }
    }

    private fun toTopic(row: Row) = Topic(
        id = row.int("id"),
        name = row.string("name"),
        topic = row.string("topic"),
        type = TopicType.valueOf(row.string("type")),
        running = row.boolean("running")
    )

    @Serializable
    data class Topic(
        val id: Int,
        val name: String,
        val topic: String,
        val type: TopicType,
        val running: Boolean
    )
}
