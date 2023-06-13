package no.nav.mulighetsrommet.api.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattDbo
import no.nav.mulighetsrommet.api.utils.DatabaseUtils
import no.nav.mulighetsrommet.api.utils.NavAnsattFilter
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import org.intellij.lang.annotations.Language
import java.util.*

class NavAnsattRepository(private val db: Database) {
    fun upsert(ansatt: NavAnsattDbo): QueryResult<NavAnsattDbo> = query {
        @Language("PostgreSQL")
        val query = """
            insert into nav_ansatt(nav_ident, fornavn, etternavn, hovedenhet, azure_id, fra_ad_gruppe, mobilnummer, epost)
            values (:nav_ident, :fornavn, :etternavn, :hovedenhet, :azure_id::uuid, :fra_ad_gruppe::uuid, :mobilnummer, :epost)
            on conflict (nav_ident, fra_ad_gruppe)
                do update set fornavn       = excluded.fornavn,
                              etternavn     = excluded.etternavn,
                              hovedenhet    = excluded.hovedenhet,
                              azure_id      = excluded.azure_id,
                              fra_ad_gruppe = excluded.fra_ad_gruppe,
                              mobilnummer   = excluded.mobilnummer,
                              epost         = excluded.epost
            returning *
        """.trimIndent()

        queryOf(query, ansatt.toSqlParameters())
            .map { it.toNavAnsatt() }
            .asSingle
            .let { db.run(it)!! }
    }

    fun getAll(filter: NavAnsattFilter): QueryResult<List<NavAnsattDbo>> = query {
        val params = mapOf(
            "azureIder" to db.createUuidArray(filter.azureIder),
        )

        val where = DatabaseUtils.andWhereParameterNotNull(
            filter.azureIder to "fra_ad_gruppe = any(:azureIder)",
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

    fun getByNavIdentAndAdGruppe(navIdent: String, adGruppe: UUID): QueryResult<NavAnsattDbo?> = query {
        @Language("PostgreSQL")
        val query = """
            select nav_ident, fornavn, etternavn, hovedenhet, azure_id, fra_ad_gruppe, mobilnummer, epost
            from nav_ansatt
            where nav_ident = :nav_ident and fra_ad_gruppe = :fra_ad_gruppe::uuid
        """.trimIndent()

        queryOf(query, mapOf("nav_ident" to navIdent, "fra_ad_gruppe" to adGruppe))
            .map { it.toNavAnsatt() }
            .asSingle
            .let { db.run(it) }
    }

    fun getByAzureIdAndAdGruppe(azureId: UUID, adGruppe: UUID): QueryResult<NavAnsattDbo?> = query {
        @Language("PostgreSQL")
        val query = """
            select nav_ident, fornavn, etternavn, hovedenhet, azure_id, fra_ad_gruppe, mobilnummer, epost
            from nav_ansatt
            where azure_id = :azure_id::uuid and fra_ad_gruppe = :fra_ad_gruppe::uuid
        """.trimIndent()

        queryOf(query, mapOf("azure_id" to azureId, "fra_ad_gruppe" to adGruppe))
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
        "mobilnummer" to mobilnummer,
        "epost" to epost,
    )

    private fun Row.toNavAnsatt() = NavAnsattDbo(
        navIdent = string("nav_ident"),
        fornavn = string("fornavn"),
        etternavn = string("etternavn"),
        hovedenhet = string("hovedenhet"),
        azureId = uuid("azure_id"),
        fraAdGruppe = uuid("fra_ad_gruppe"),
        mobilnummer = stringOrNull("mobilnummer"),
        epost = string("epost"),
    )
}
