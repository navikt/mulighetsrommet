package no.nav.mulighetsrommet.api.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattDbo
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import org.intellij.lang.annotations.Language
import java.util.*

class NavAnsattRepository(private val db: Database) {
    fun upsert(ansatt: NavAnsattDbo): QueryResult<NavAnsattDbo> = query {
        @Language("PostgreSQL")
        val query = """
            insert into nav_ansatt(nav_ident, oid, fornavn, etternavn, hovedenhet)
            values (:nav_ident, :oid, :fornavn, :etternavn, :hovedenhet)
            on conflict (nav_ident)
                do update set oid        = excluded.oid,
                              fornavn    = excluded.fornavn,
                              etternavn  = excluded.etternavn,
                              hovedenhet = excluded.hovedenhet
            returning *
        """.trimIndent()

        queryOf(query, ansatt.toSqlParameters())
            .map { it.toNavAnsatt() }
            .asSingle
            .let { db.run(it)!! }
    }

    fun getByNavIdent(navIdent: String): QueryResult<NavAnsattDbo?> = query {
        @Language("PostgreSQL")
        val query = """
            select nav_ident, oid, fornavn, etternavn, hovedenhet
            from nav_ansatt
            where nav_ident = :nav_ident
        """.trimIndent()

        queryOf(query, mapOf("nav_ident" to navIdent))
            .map { it.toNavAnsatt() }
            .asSingle
            .let { db.run(it) }
    }

    fun getByObjectId(objectId: UUID): QueryResult<NavAnsattDbo?> = query {
        @Language("PostgreSQL")
        val query = """
            select nav_ident, oid, fornavn, etternavn, hovedenhet
            from nav_ansatt
            where oid = :oid::uuid
        """.trimIndent()

        queryOf(query, mapOf("oid" to objectId))
            .map { it.toNavAnsatt() }
            .asSingle
            .let { db.run(it) }
    }

    fun deleteByObjectId(objectId: UUID): QueryResult<Int> = query {
        @Language("PostgreSQL")
        val query = """
            delete from nav_ansatt
            where oid = :oid::uuid
        """.trimIndent()

        queryOf(query, mapOf("oid" to objectId))
            .asUpdate
            .let { db.run(it) }
    }

    private fun NavAnsattDbo.toSqlParameters() = mapOf(
        "nav_ident" to navIdent,
        "oid" to oid,
        "fornavn" to fornavn,
        "etternavn" to etternavn,
        "hovedenhet" to hovedenhet,
    )

    private fun Row.toNavAnsatt() = NavAnsattDbo(
        navIdent = string("nav_ident"),
        oid = uuid("oid"),
        fornavn = string("fornavn"),
        etternavn = string("etternavn"),
        hovedenhet = string("hovedenhet"),
    )
}
