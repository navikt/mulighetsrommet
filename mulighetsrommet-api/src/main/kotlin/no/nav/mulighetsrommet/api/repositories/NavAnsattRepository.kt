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
            insert into nav_ansatt(nav_ident, fornavn, etternavn, hovedenhet, azure_id, fra_ad_gruppe)
            values (:nav_ident, :fornavn, :etternavn, :hovedenhet, :azure_id::uuid, :fra_ad_gruppe::uuid)
            on conflict (nav_ident)
                do update set fornavn       = excluded.fornavn,
                              etternavn     = excluded.etternavn,
                              hovedenhet    = excluded.hovedenhet,
                              azure_id      = excluded.azure_id,
                              fra_ad_gruppe = excluded.fra_ad_gruppe
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
            select nav_ident, fornavn, etternavn, hovedenhet, azure_id, fra_ad_gruppe
            from nav_ansatt
            where nav_ident = :nav_ident
        """.trimIndent()

        queryOf(query, mapOf("nav_ident" to navIdent))
            .map { it.toNavAnsatt() }
            .asSingle
            .let { db.run(it) }
    }

    fun getByAzureId(azureId: UUID): QueryResult<NavAnsattDbo?> = query {
        @Language("PostgreSQL")
        val query = """
            select nav_ident, fornavn, etternavn, hovedenhet, azure_id, fra_ad_gruppe
            from nav_ansatt
            where azure_id = :azure_id::uuid
        """.trimIndent()

        queryOf(query, mapOf("azure_id" to azureId))
            .map { it.toNavAnsatt() }
            .asSingle
            .let { db.run(it) }
    }

    fun deleteByAzureId(azureId: UUID): QueryResult<Int> = query {
        @Language("PostgreSQL")
        val query = """
            delete from nav_ansatt
            where azure_id = :azure_id::uuid
        """.trimIndent()

        queryOf(query, mapOf("azure_id" to azureId))
            .asUpdate
            .let { db.run(it) }
    }

    private fun NavAnsattDbo.toSqlParameters() = mapOf(
        "nav_ident" to navIdent,
        "fornavn" to fornavn,
        "etternavn" to etternavn,
        "hovedenhet" to hovedenhet,
        "azure_id" to azureId,
        "fra_ad_gruppe" to fraAdGruppe,
    )

    private fun Row.toNavAnsatt() = NavAnsattDbo(
        navIdent = string("nav_ident"),
        fornavn = string("fornavn"),
        etternavn = string("etternavn"),
        hovedenhet = string("hovedenhet"),
        azureId = uuid("azure_id"),
        fraAdGruppe = uuid("fra_ad_gruppe"),
    )
}
