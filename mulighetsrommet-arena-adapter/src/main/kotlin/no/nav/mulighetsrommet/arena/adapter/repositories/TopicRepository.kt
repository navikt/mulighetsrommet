package no.nav.mulighetsrommet.arena.adapter.repositories

import kotlinx.serialization.Serializable
import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.arena.adapter.Database
import no.nav.mulighetsrommet.arena.adapter.kafka.TopicConsumer
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

    // TODO: https://github.com/seratch/kotliquery/issues/54 - Bug med at den ikke returnerer rader
    // Denne skal kunne returnere bare rader som har blitt oppdatert, men gitt bug over går det ikke
    // Filterer ut det manuelt i ManagerService isteden.
    fun updateRunning(topics: List<Topic>) {
        @Language("PostgreSQL")
        val query = """
            update topics set running = ? where id = ? and running != ? returning *
        """.trimIndent()
        db.transaction { tx ->
            topics.forEach {
                tx.run(queryOf(query, it.running, it.id, it.running).asExecute)
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
            returning * 
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
