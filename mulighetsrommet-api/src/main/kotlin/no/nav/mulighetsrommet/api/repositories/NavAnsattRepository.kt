package no.nav.mulighetsrommet.api.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattRolle
import no.nav.mulighetsrommet.api.utils.DatabaseUtils
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import org.intellij.lang.annotations.Language
import java.util.*

class NavAnsattRepository(private val db: Database) {
    fun upsert(ansatt: NavAnsattDbo): QueryResult<NavAnsattDbo> = query {
        @Language("PostgreSQL")
        val query = """
            insert into nav_ansatt(nav_ident, fornavn, etternavn, hovedenhet, azure_id, mobilnummer, epost, roller)
            values (:nav_ident, :fornavn, :etternavn, :hovedenhet, :azure_id::uuid, :mobilnummer, :epost, :roller::nav_ansatt_rolle[])
            on conflict (nav_ident)
                do update set fornavn       = excluded.fornavn,
                              etternavn     = excluded.etternavn,
                              hovedenhet    = excluded.hovedenhet,
                              azure_id      = excluded.azure_id,
                              mobilnummer   = excluded.mobilnummer,
                              epost         = excluded.epost,
                              roller        = excluded.roller
            returning *
        """.trimIndent()

        queryOf(query, ansatt.toSqlParameters())
            .map { it.toNavAnsatt() }
            .asSingle
            .let { db.run(it)!! }
    }

    fun getAll(roller: List<NavAnsattRolle>? = null): QueryResult<List<NavAnsattDbo>> = query {
        val params = mapOf(
            "roller" to roller?.let { roller -> db.createTextArray(roller.map { it.name }) },
        )

        val where = DatabaseUtils.andWhereParameterNotNull(
            roller to "roller @> :roller::nav_ansatt_rolle[]",
        )

        @Language("PostgreSQL")
        val query = """
            select *
            from nav_ansatt
            $where
        """.trimIndent()

        queryOf(query, params)
            .map { it.toNavAnsatt() }
            .asList
            .let { db.run(it) }
    }

    fun getByNavIdent(navIdent: String): QueryResult<NavAnsattDbo?> = query {
        @Language("PostgreSQL")
        val query = """
            select nav_ident, fornavn, etternavn, hovedenhet, azure_id, mobilnummer, epost, roller
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
            select nav_ident, fornavn, etternavn, hovedenhet, azure_id, mobilnummer, epost, roller
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
        "mobilnummer" to mobilnummer,
        "epost" to epost,
        "roller" to db.createArrayOf("nav_ansatt_rolle", roller.map { it.name }),
    )

    private fun Row.toNavAnsatt() = NavAnsattDbo(
        navIdent = string("nav_ident"),
        fornavn = string("fornavn"),
        etternavn = string("etternavn"),
        hovedenhet = string("hovedenhet"),
        azureId = uuid("azure_id"),
        mobilnummer = stringOrNull("mobilnummer"),
        epost = string("epost"),
        roller = array<String>("roller").toList().map { NavAnsattRolle.valueOf(it) },
    )
}
