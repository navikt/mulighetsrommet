package no.nav.mulighetsrommet.api.navansatt.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsattDto
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.DatabaseUtils
import no.nav.mulighetsrommet.domain.dto.NavIdent
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import java.util.*

class NavAnsattRepository(private val db: Database) {
    fun upsert(ansatt: NavAnsattDbo) =
        db.transaction { tx ->
            upsert(ansatt, tx)
        }

    fun upsert(ansatt: NavAnsattDbo, tx: Session) {
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

        tx.run(queryOf(query, ansatt.toSqlParameters()).asExecute)
    }

    fun getAll(
        roller: List<NavAnsattRolle>? = null,
        hovedenhetIn: List<String>? = null,
        skalSlettesDatoLte: LocalDate? = null,
    ): List<NavAnsattDto> {
        val params = mapOf(
            "roller" to roller?.let { db.createTextArray(it.map { rolle -> rolle.name }) },
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

        return queryOf(query, params)
            .map { it.toNavAnsattDto() }
            .asList
            .let { db.run(it) }
    }

    fun getByNavIdent(navIdent: NavIdent): NavAnsattDto? {
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

        return queryOf(query, mapOf("nav_ident" to navIdent.value))
            .map { it.toNavAnsattDto() }
            .asSingle
            .let { db.run(it) }
    }

    fun getByAzureId(azureId: UUID): NavAnsattDto? {
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

        return queryOf(query, mapOf("azure_id" to azureId))
            .map { it.toNavAnsattDto() }
            .asSingle
            .let { db.run(it) }
    }

    fun deleteByAzureId(azureId: UUID, tx: Session? = null): Int {
        @Language("PostgreSQL")
        val query = """
            delete from nav_ansatt
            where azure_id = :azure_id::uuid
        """.trimIndent()

        val update = queryOf(query, mapOf("azure_id" to azureId)).asUpdate

        return tx?.run(update) ?: db.run(update)
    }

    private fun NavAnsattDbo.toSqlParameters() = mapOf(
        "nav_ident" to navIdent.value,
        "fornavn" to fornavn,
        "etternavn" to etternavn,
        "hovedenhet" to hovedenhet,
        "azure_id" to azureId,
        "mobilnummer" to mobilnummer,
        "epost" to epost,
        "roller" to db.createArrayOf("nav_ansatt_rolle", roller),
        "skal_slettes_dato" to skalSlettesDato,
    )

    private fun Row.toNavAnsattDto() = NavAnsattDto(
        navIdent = NavIdent(string("nav_ident")),
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
