package no.nav.mulighetsrommet.api.repositories

import kotliquery.queryOf
import no.nav.mulighetsrommet.api.domain.dto.JoyrideType
import no.nav.mulighetsrommet.api.domain.dto.VeilederJoyrideDto
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import org.intellij.lang.annotations.Language

class VeilederJoyrideRepository(private val db: Database) {

    fun upsert(data: VeilederJoyrideDto): QueryResult<Unit> = query {
        @Language("PostgreSQL")
        val query = """
            insert into veileder_joyride(
                nav_ident, fullfort, type
            ) values (:nav_ident, :fullfort, :type::joyride_type)
            on conflict (nav_ident, type) do update
                set nav_ident = excluded.nav_ident,
                    fullfort = excluded.fullfort,
                    type = excluded.type::joyride_type
        """.trimIndent()

        queryOf(query, data.toSqlParameters()).asExecute.let { db.run(it) }
    }

    fun harFullfortJoyride(navIdent: String, type: JoyrideType): Boolean {
        val params = mapOf(
            "nav_ident" to navIdent,
            "type" to type.name,
        )

        @Language("PostgreSQL")
        val query = """
            select fullfort from veileder_joyride where nav_ident = :nav_ident and type = :type::joyride_type
        """.trimIndent()

        return queryOf(query, params).map { it.boolean("fullfort") }.asSingle.let { db.run(it) } ?: false
    }

    private fun VeilederJoyrideDto.toSqlParameters() = mapOf(
        "nav_ident" to navIdent,
        "fullfort" to fullfort,
        "type" to type.name,
    )
}
