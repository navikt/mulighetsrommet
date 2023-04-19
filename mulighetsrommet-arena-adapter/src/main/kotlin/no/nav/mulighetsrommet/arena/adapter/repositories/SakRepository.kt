package no.nav.mulighetsrommet.arena.adapter.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.arena.adapter.models.db.Sak
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import org.intellij.lang.annotations.Language
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SakRepository(private val db: Database) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun upsert(sak: Sak): QueryResult<Sak> = query {
        logger.info("Lagrer sak id=${sak.sakId}")

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

        queryOf(query, sak.toSqlParameters())
            .map { it.toSak() }
            .asSingle
            .let { db.run(it)!! }
    }

    fun delete(id: Int): QueryResult<Unit> = query {
        @Language("PostgreSQL")
        val query = """
            delete from sak
            where sak_id = ?
        """.trimIndent()

        queryOf(query, id)
            .asExecute
            .let { db.run(it) }
    }

    fun get(id: Int): Sak? {
        logger.info("Henter sak id=$id")

        @Language("PostgreSQL")
        val query = """
            select sak_id, lopenummer, aar, enhet
            from sak
            where sak_id = ?
        """.trimIndent()

        return queryOf(query, id)
            .map { it.toSak() }
            .asSingle
            .let { db.run(it) }
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
        enhet = string("enhet"),
    )
}
