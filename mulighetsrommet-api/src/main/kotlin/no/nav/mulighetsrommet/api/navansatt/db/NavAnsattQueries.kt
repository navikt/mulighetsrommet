package no.nav.mulighetsrommet.api.navansatt.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsattDto
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsattRolle
import no.nav.mulighetsrommet.database.createTextArray
import no.nav.mulighetsrommet.database.withTransaction
import no.nav.mulighetsrommet.model.NavIdent
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import java.util.*

class NavAnsattQueries(private val session: Session) {

    fun upsert(ansatt: NavAnsattDbo) = withTransaction(session) {
        @Language("PostgreSQL")
        val query = """
            insert into nav_ansatt(nav_ident, fornavn, etternavn, hovedenhet, azure_id, mobilnummer, epost, skal_slettes_dato)
            values (:nav_ident, :fornavn, :etternavn, :hovedenhet, :azure_id::uuid, :mobilnummer, :epost, :skal_slettes_dato)
            on conflict (nav_ident)
                do update set fornavn           = excluded.fornavn,
                              etternavn         = excluded.etternavn,
                              hovedenhet        = excluded.hovedenhet,
                              azure_id          = excluded.azure_id,
                              mobilnummer       = excluded.mobilnummer,
                              epost             = excluded.epost,
                              skal_slettes_dato = excluded.skal_slettes_dato
        """.trimIndent()
        val params = mapOf(
            "nav_ident" to ansatt.navIdent.value,
            "fornavn" to ansatt.fornavn,
            "etternavn" to ansatt.etternavn,
            "hovedenhet" to ansatt.hovedenhet,
            "azure_id" to ansatt.azureId,
            "mobilnummer" to ansatt.mobilnummer,
            "epost" to ansatt.epost,
            "skal_slettes_dato" to ansatt.skalSlettesDato,
        )
        execute(queryOf(query, params))

        @Language("PostgreSQL")
        val deleteRoles = """
            delete from nav_ansatt_rolle
            where nav_ansatt_nav_ident = ?
        """.trimIndent()
        execute(queryOf(deleteRoles, ansatt.navIdent.value))

        if (ansatt.roller.isNotEmpty()) {
            @Language("PostgreSQL")
            val insertRoles = """
                insert into nav_ansatt_rolle(nav_ansatt_nav_ident, rolle)
                values (:nav_ident, :rolle::rolle)
            """.trimIndent()
            batchPreparedNamedStatement(
                insertRoles,
                ansatt.roller.map { mapOf("nav_ident" to ansatt.navIdent.value, "rolle" to it.name) },
            )
        }
    }

    fun getAll(
        roller: List<NavAnsattRolle>? = null,
        hovedenhetIn: List<String>? = null,
        skalSlettesDatoLte: LocalDate? = null,
    ): List<NavAnsattDto> = with(session) {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_nav_ansatt_dto
            where (:roller::rolle[] is null or roller @> :roller)
              and (:hovedenhet::text[] is null or hovedenhet_enhetsnummer = any(:hovedenhet))
              and (:skal_slettes_dato::date is null or skal_slettes_dato <= :skal_slettes_dato)
            order by fornavn, etternavn
        """.trimIndent()

        val params = mapOf(
            "roller" to roller?.map { it.name }?.let { createArrayOf("rolle", it) },
            "hovedenhet" to hovedenhetIn?.let { createTextArray(it) },
            "skal_slettes_dato" to skalSlettesDatoLte,
        )

        return list(queryOf(query, params)) { it.toNavAnsattDto() }
    }

    fun getByNavIdent(navIdent: NavIdent): NavAnsattDto? = with(session) {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_nav_ansatt_dto
            where nav_ident = ?
        """.trimIndent()

        return single(queryOf(query, navIdent.value)) { it.toNavAnsattDto() }
    }

    fun getByAzureId(azureId: UUID): NavAnsattDto? = with(session) {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_nav_ansatt_dto
            where azure_id = ?::uuid
        """.trimIndent()

        return single(queryOf(query, azureId)) { it.toNavAnsattDto() }
    }

    fun deleteByAzureId(azureId: UUID): Int = with(session) {
        @Language("PostgreSQL")
        val query = """
            delete from nav_ansatt
            where azure_id = ?::uuid
        """.trimIndent()

        return update(queryOf(query, azureId))
    }

    private fun Row.toNavAnsattDto() = NavAnsattDto(
        navIdent = NavIdent(string("nav_ident")),
        fornavn = string("fornavn"),
        etternavn = string("etternavn"),
        hovedenhet = NavAnsattDto.Hovedenhet(
            enhetsnummer = string("hovedenhet_enhetsnummer"),
            navn = string("hovedenhet_navn"),
        ),
        azureId = uuid("azure_id"),
        mobilnummer = stringOrNull("mobilnummer"),
        epost = string("epost"),
        roller = array<String?>("roller").filterNotNull().map { NavAnsattRolle.valueOf(it) }.toSet(),
        skalSlettesDato = localDateOrNull("skal_slettes_dato"),
    )
}
