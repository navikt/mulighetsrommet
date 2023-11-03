package no.nav.mulighetsrommet.api.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattRolle
import no.nav.mulighetsrommet.api.domain.dto.NavAnsattDto
import no.nav.mulighetsrommet.api.utils.DatabaseUtils
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import java.util.*

class NavAnsattRepository(private val db: Database) {

    fun upsert(ansatt: NavAnsattDbo): QueryResult<NavAnsattDbo> = query {
        @Language("PostgreSQL")
        val query = """
            insert into nav_ansatt(nav_ident, fornavn, etternavn, hovedenhet, azure_id, mobilnummer, epost, roller, skal_slettes_dato)
            values (:nav_ident, :fornavn, :etternavn, :hovedenhet, :azure_id::uuid, :mobilnummer, :epost, :roller::nav_ansatt_rolle[], :skal_slettes_dato)
            on conflict (nav_ident)
                do update set fornavn           = excluded.fornavn,
                              etternavn         = excluded.etternavn,
                              hovedenhet        = excluded.hovedenhet,
                              azure_id          = excluded.azure_id,
                              mobilnummer       = excluded.mobilnummer,
                              epost             = excluded.epost,
                              roller            = excluded.roller,
                              skal_slettes_dato = excluded.skal_slettes_dato
            returning *
        """.trimIndent()

        queryOf(query, ansatt.toSqlParameters())
            .map { it.toNavAnsatt() }
            .asSingle
            .let { db.run(it)!! }
    }

    fun getAll(
        roller: List<NavAnsattRolle>? = null,
        hovedenhetIn: List<String>? = null,
        skalSlettesDatoLte: LocalDate? = null,
    ): QueryResult<List<NavAnsattDto>> = query {
        val params = mapOf(
            "roller" to roller?.let { roller -> db.createTextArray(roller.map { it.name }) },
            "hovedenhet" to hovedenhetIn?.let { enheter -> db.createTextArray(enheter) },
            "skal_slettes_dato" to skalSlettesDatoLte,
        )

        val where = DatabaseUtils.andWhereParameterNotNull(
            roller to "roller @> :roller::nav_ansatt_rolle[]",
            hovedenhetIn to "hovedenhet = any(:hovedenhet)",
            skalSlettesDatoLte to "skal_slettes_dato <= :skal_slettes_dato",
        )

        @Language("PostgreSQL")
        val query = """
            select nav_ident,
                   fornavn,
                   etternavn,
                   enhetsnummer,
                   ne.navn as enhetsnavn,
                   azure_id,
                   mobilnummer,
                   epost,
                   roller,
                   skal_slettes_dato
            from nav_ansatt
                     join nav_enhet ne on nav_ansatt.hovedenhet = ne.enhetsnummer
            $where
            order by fornavn, etternavn
        """.trimIndent()

        queryOf(query, params)
            .map { it.toNavAnsattDto() }
            .asList
            .let { db.run(it) }
    }

    fun getByNavIdent(navIdent: String): QueryResult<NavAnsattDto?> = query {
        @Language("PostgreSQL")
        val query = """
            select nav_ident,
                   fornavn,
                   etternavn,
                   enhetsnummer,
                   ne.navn as enhetsnavn,
                   azure_id,
                   mobilnummer,
                   epost,
                   roller,
                   skal_slettes_dato
            from nav_ansatt
                     join nav_enhet ne on nav_ansatt.hovedenhet = ne.enhetsnummer
            where nav_ident = :nav_ident
        """.trimIndent()

        queryOf(query, mapOf("nav_ident" to navIdent))
            .map { it.toNavAnsattDto() }
            .asSingle
            .let { db.run(it) }
    }

    fun getByAzureId(azureId: UUID): QueryResult<NavAnsattDto?> = query {
        @Language("PostgreSQL")
        val query = """
            select nav_ident,
                   fornavn,
                   etternavn,
                   enhetsnummer,
                   ne.navn as enhetsnavn,
                   azure_id,
                   mobilnummer,
                   epost,
                   roller,
                   skal_slettes_dato
            from nav_ansatt
                     join nav_enhet ne on nav_ansatt.hovedenhet = ne.enhetsnummer
            where azure_id = :azure_id::uuid
        """.trimIndent()

        queryOf(query, mapOf("azure_id" to azureId))
            .map { it.toNavAnsattDto() }
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
        "skal_slettes_dato" to skalSlettesDato,
    )

    private fun Row.toNavAnsatt() = NavAnsattDbo(
        navIdent = string("nav_ident"),
        fornavn = string("fornavn"),
        etternavn = string("etternavn"),
        hovedenhet = string("hovedenhet"),
        azureId = uuid("azure_id"),
        mobilnummer = stringOrNull("mobilnummer"),
        epost = string("epost"),
        roller = array<String>("roller").map { NavAnsattRolle.valueOf(it) }.toSet(),
        skalSlettesDato = localDateOrNull("skal_slettes_dato"),
    )

    private fun Row.toNavAnsattDto() = NavAnsattDto(
        navIdent = string("nav_ident"),
        fornavn = string("fornavn"),
        etternavn = string("etternavn"),
        hovedenhet = NavAnsattDto.Hovedenhet(
            enhetsnummer = string("enhetsnummer"),
            navn = string("enhetsnavn"),
        ),
        azureId = uuid("azure_id"),
        mobilnummer = stringOrNull("mobilnummer"),
        epost = string("epost"),
        roller = array<String>("roller").map { NavAnsattRolle.valueOf(it) }.toSet(),
        skalSlettesDato = localDateOrNull("skal_slettes_dato"),
    )
}
