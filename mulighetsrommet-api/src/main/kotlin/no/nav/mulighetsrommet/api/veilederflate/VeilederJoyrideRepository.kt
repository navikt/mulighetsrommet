package no.nav.mulighetsrommet.api.veilederflate

import kotliquery.queryOf
import no.nav.mulighetsrommet.api.veilederflate.models.JoyrideType
import no.nav.mulighetsrommet.api.veilederflate.models.VeilederJoyrideDto
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.dto.NavIdent
import org.intellij.lang.annotations.Language

class VeilederJoyrideRepository(private val db: Database) {

    fun upsert(data: VeilederJoyrideDto): QueryResult<Unit> = query {
        @Language("PostgreSQL")
        val query = """
            insert into veileder_joyride(
                nav_ident, fullfort, type
            ) values (:nav_ident, :fullfort, :type::joyride_type)
            on conflict (nav_ident, type) do update
                set fullfort = excluded.fullfort
        """.trimIndent()

        queryOf(query, data.toSqlParameters()).asExecute.let { db.run(it) }
    }

    fun harFullfortJoyride(navIdent: NavIdent, type: JoyrideType): Boolean {
        val params = mapOf(
            "nav_ident" to navIdent.value,
            "type" to type.name,
        )

        @Language("PostgreSQL")
        val query = """
            select fullfort from veileder_joyride where nav_ident = :nav_ident and type = :type::joyride_type
        """.trimIndent()

        return queryOf(query, params).map { it.boolean("fullfort") }.asSingle.let { db.run(it) } ?: false
    }

    private fun VeilederJoyrideDto.toSqlParameters() = mapOf(
        "nav_ident" to navIdent.value,
        "fullfort" to fullfort,
        "type" to type.name,
    )
}
