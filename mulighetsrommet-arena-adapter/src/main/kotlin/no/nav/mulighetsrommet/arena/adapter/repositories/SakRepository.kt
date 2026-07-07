package no.nav.mulighetsrommet.arena.adapter.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.arena.adapter.models.db.Sak
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.requireSingle
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import org.intellij.lang.annotations.Language

class SakRepository(private val db: Database) {
    fun upsert(sak: Sak): QueryResult<Sak> = query {
        @Language("PostgreSQL")
        val query = """
            insert into sak(sak_id, lopenummer, aar, enhet)
            values (:sak_id, :lopenummer, :aar, :enhet)
            on conflict (sak_id)
                do update set lopenummer = excluded.lopenummer,
                              aar        = excluded.aar,
                              enhet      = excluded.enhet
            returning *
        """.trimIndent()

        db.session { session ->
            session.requireSingle(queryOf(query, sak.toSqlParameters())) { it.toSak() }
        }
    }

    fun delete(id: Int): QueryResult<Unit> = query {
        @Language("PostgreSQL")
        val query = """
            delete from sak
            where sak_id = ?
        """.trimIndent()

        db.session { session ->
            session.execute(queryOf(query, id))
        }
    }

    fun get(id: Int): Sak? = db.session { session ->
        @Language("PostgreSQL")
        val query = """
            select sak_id, lopenummer, aar, enhet
            from sak
            where sak_id = ?
        """.trimIndent()

        return session.single(queryOf(query, id)) { it.toSak() }
    }

    private fun Sak.toSqlParameters() = mapOf(
        "sak_id" to sakId,
        "lopenummer" to lopenummer,
        "aar" to aar,
        "enhet" to enhet,
    )

    private fun Row.toSak() = Sak(
        sakId = int("sak_id"),
        lopenummer = int("lopenummer"),
        aar = int("aar"),
        enhet = stringOrNull("enhet"),
    )
}
