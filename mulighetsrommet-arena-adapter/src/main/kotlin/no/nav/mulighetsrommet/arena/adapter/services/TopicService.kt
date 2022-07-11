package no.nav.mulighetsrommet.arena.adapter.services

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.arena.adapter.Database
import org.intellij.lang.annotations.Language

class TopicService(private val db: Database) {
    fun getTopics(): List<String> {
        @Language("PostgreSQL")
        val query = """
            select topic from events group by topic
        """.trimIndent()

        return queryOf(query)
            .map { toTopic(it) }
            .asList
            .let { db.run(it) }
    }

    private fun toTopic(row: Row): String {
        return row.string("topic")
    }
}
