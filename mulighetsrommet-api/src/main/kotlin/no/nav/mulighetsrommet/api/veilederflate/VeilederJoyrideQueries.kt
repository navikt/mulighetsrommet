package no.nav.mulighetsrommet.api.veilederflate

import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.veilederflate.models.JoyrideType
import no.nav.mulighetsrommet.api.veilederflate.models.VeilederJoyrideDto
import no.nav.mulighetsrommet.model.NavIdent
import org.intellij.lang.annotations.Language

class VeilederJoyrideQueries(private val session: Session) {

    fun upsert(dto: VeilederJoyrideDto) {
        @Language("PostgreSQL")
        val query = """
            insert into veileder_joyride(
                nav_ident, fullfort, type
            ) values (:nav_ident, :fullfort, :type::joyride_type)
            on conflict (nav_ident, type) do update
                set fullfort = excluded.fullfort
        """.trimIndent()

        val params = mapOf(
            "nav_ident" to dto.navIdent.value,
            "fullfort" to dto.fullfort,
            "type" to dto.type.name,
        )

        session.execute(queryOf(query, params))
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

        return session.single(queryOf(query, params)) { it.boolean("fullfort") } ?: false
    }
}
