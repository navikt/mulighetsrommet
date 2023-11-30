package no.nav.mulighetsrommet.api.repositories

import kotliquery.queryOf
import no.nav.mulighetsrommet.api.domain.dto.JoyrideType
import no.nav.mulighetsrommet.api.domain.dto.VeilederJoyrideDto
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import org.intellij.lang.annotations.Language

class VeilederJoyrideRepository(private val db: Database) {

    fun save(data: VeilederJoyrideDto): QueryResult<Unit> = query {
        @Language("PostgreSQL")
        val query = """
            insert into veileder_joyride(
                navident, fullfort, type
            ) values (:navident, :fullfort, :type::joyride_type)
            returning *
        """.trimIndent()

        queryOf(query, data.toSqlParameters()).asExecute.let { db.run(it) }
    }

    fun harFullfortJoyride(navident: String, type: JoyrideType): Boolean {
        val params = mapOf(
            "navident" to navident,
            "type" to type.name,
        )

        @Language("PostgreSQL")
        val query = """
            select fullfort from veileder_joyride where navident = :navident and type = :type::joyride_type
        """.trimIndent()

        return queryOf(query, params).map { it.boolean("fullfort") }.asSingle.let { db.run(it) } ?: false
    }

    private fun VeilederJoyrideDto.toSqlParameters() = mapOf(
        "navident" to navident,
        "fullfort" to fullfort,
        "type" to type.name,
    )
}
