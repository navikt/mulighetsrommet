package no.nav.mulighetsrommet.api.persistence.redaksjoneltinnhold

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.domain.redaksjoneltinnhold.RedaksjoneltInnholdLenke
import no.nav.mulighetsrommet.api.domain.redaksjoneltinnhold.RedaksjoneltInnholdLenkeRepository
import no.nav.mulighetsrommet.database.requireSingle
import org.intellij.lang.annotations.Language
import java.util.UUID

class RedaksjoneltInnholdLenkeQueries(private val session: Session) : RedaksjoneltInnholdLenkeRepository {

    override fun upsert(lenke: RedaksjoneltInnholdLenke): RedaksjoneltInnholdLenke {
        @Language("PostgreSQL")
        val query = """
            insert into redaksjonelt_innhold_lenke (id, url, navn, beskrivelse)
            values (:id::uuid, :url, :navn, :beskrivelse)
            on conflict (id) do update set
                url         = excluded.url,
                navn        = excluded.navn,
                beskrivelse = excluded.beskrivelse
            returning id, url, navn, beskrivelse
        """.trimIndent()

        val params = mapOf(
            "id" to lenke.id,
            "url" to lenke.url,
            "navn" to lenke.navn,
            "beskrivelse" to lenke.beskrivelse,
        )
        return session.requireSingle(queryOf(query, params)) { it.toRedaksjoneltInnholdLenke() }
    }

    override fun getAll(): List<RedaksjoneltInnholdLenke> {
        @Language("PostgreSQL")
        val query = """
            select id, url, navn, beskrivelse
            from redaksjonelt_innhold_lenke
            order by url
        """.trimIndent()

        return session.list(queryOf(query)) { it.toRedaksjoneltInnholdLenke() }
    }

    override fun get(id: UUID): RedaksjoneltInnholdLenke? {
        @Language("PostgreSQL")
        val query = """
            select id, url, navn, beskrivelse
            from redaksjonelt_innhold_lenke
            where id = ?::uuid
        """.trimIndent()

        return session.single(queryOf(query, id)) { it.toRedaksjoneltInnholdLenke() }
    }

    override fun delete(id: UUID): Int {
        @Language("PostgreSQL")
        val query = """
            delete from redaksjonelt_innhold_lenke where id = ?::uuid
        """.trimIndent()

        return session.update(queryOf(query, id))
    }
}

private fun Row.toRedaksjoneltInnholdLenke() = RedaksjoneltInnholdLenke(
    id = uuid("id"),
    url = string("url"),
    navn = stringOrNull("navn"),
    beskrivelse = stringOrNull("beskrivelse"),
)
