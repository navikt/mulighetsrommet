package no.nav.mulighetsrommet.api.navansatt.db

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsatt
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsattRolle
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.database.createArrayOfValue
import no.nav.mulighetsrommet.database.requireSingle
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import org.intellij.lang.annotations.Language
import java.sql.Array
import java.time.LocalDate
import java.util.*

class NavAnsattQueries(private val session: Session) {

    fun upsert(ansatt: NavAnsattDbo) {
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
            "hovedenhet" to ansatt.hovedenhet.value,
            "azure_id" to ansatt.azureId,
            "mobilnummer" to ansatt.mobilnummer,
            "epost" to ansatt.epost,
            "skal_slettes_dato" to ansatt.skalSlettesDato,
        )
        session.execute(queryOf(query, params))
    }

    fun setRoller(navIdent: NavIdent, roller: Set<NavAnsattRolle>) {
        @Language("PostgreSQL")
        val deleteRoles = """
            delete from nav_ansatt_rolle
            where nav_ansatt_nav_ident = ?
              and not (rolle = any(?::rolle[]))
        """.trimIndent()
        session.execute(
            queryOf(
                deleteRoles,
                navIdent.value,
                session.createArrayOfRolle(roller.map { it.rolle }),
            ),
        )

        if (roller.isNotEmpty()) {
            @Language("PostgreSQL")
            val insertRolle = """
                insert into nav_ansatt_rolle(nav_ansatt_nav_ident, rolle)
                values (:nav_ident, :rolle::rolle)
                on conflict (nav_ansatt_nav_ident, rolle) do nothing;
            """.trimIndent()

            @Language("PostgreSQL")
            val selectRolleId = """
                select id
                from nav_ansatt_rolle
                where nav_ansatt_nav_ident = :nav_ident
                  and rolle = :rolle::rolle;
            """.trimIndent()

            @Language("PostgreSQL")
            val deleteEnheter = """
                delete from nav_ansatt_rolle_nav_enhet
                where nav_ansatt_rolle_id = ?
                  and not (nav_enhet_enhetsnummer = any(?::text[]))
            """.trimIndent()

            @Language("PostgreSQL")
            val insertRolleEnhet = """
                insert into nav_ansatt_rolle_nav_enhet (nav_ansatt_rolle_id, nav_enhet_enhetsnummer)
                values (:role_id, :enhet)
                on conflict (nav_ansatt_rolle_id, nav_enhet_enhetsnummer) do nothing
            """.trimIndent()

            roller.forEach { rolle ->
                val paramsRolle = mapOf("nav_ident" to navIdent.value, "rolle" to rolle.rolle.name)
                session.execute(queryOf(insertRolle, paramsRolle))

                when (rolle) {
                    is NavAnsattRolle.Generell -> Unit

                    is NavAnsattRolle.Kontorspesifikk -> {
                        val id = session.requireSingle(queryOf(selectRolleId, paramsRolle)) { it.int("id") }

                        session.execute(
                            queryOf(deleteEnheter, id, session.createArrayOfValue(rolle.enheter) { it.value }),
                        )

                        val paramsRolleEnheter = rolle.enheter.map { mapOf("role_id" to id, "enhet" to it.value) }
                        session.batchPreparedNamedStatement(insertRolleEnhet, paramsRolleEnheter)
                    }
                }
            }
        }
    }

    fun getAll(
        rollerContainsAll: List<NavAnsattRolle>? = null,
        hovedenhetIn: List<NavEnhetNummer>? = null,
        skalSlettesDatoLte: LocalDate? = null,
    ): List<NavAnsatt> = with(session) {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_nav_ansatt_dto
            where (:roller::jsonb is null or (roller_json @> :roller::jsonb))
              and (:hovedenhet::text[] is null or hovedenhet_enhetsnummer = any(:hovedenhet))
              and (:skal_slettes_dato::date is null or skal_slettes_dato <= :skal_slettes_dato)
            order by fornavn, etternavn
        """.trimIndent()

        val params = mapOf(
            "roller" to rollerContainsAll?.let { Json.encodeToString(it) },
            "hovedenhet" to hovedenhetIn?.let { createArrayOfValue(it) { it.value } },
            "skal_slettes_dato" to skalSlettesDatoLte,
        )

        return list(queryOf(query, params)) { it.toNavAnsattDto() }
    }

    fun getByNavIdent(navIdent: NavIdent): NavAnsatt? = with(session) {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_nav_ansatt_dto
            where nav_ident = ?
        """.trimIndent()

        return single(queryOf(query, navIdent.value)) { it.toNavAnsattDto() }
    }

    fun getByAzureId(azureId: UUID): NavAnsatt? = with(session) {
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
}

private fun Row.toNavAnsattDto(): NavAnsatt {
    val roller = stringOrNull("roller_json")
        ?.let { JsonIgnoreUnknownKeys.decodeFromString<Set<NavAnsattRolle>>(it) }
        ?: setOf<NavAnsattRolle>()
    return NavAnsatt(
        navIdent = NavIdent(string("nav_ident")),
        fornavn = string("fornavn"),
        etternavn = string("etternavn"),
        hovedenhet = NavAnsatt.Hovedenhet(
            enhetsnummer = NavEnhetNummer(string("hovedenhet_enhetsnummer")),
            navn = string("hovedenhet_navn"),
        ),
        azureId = uuid("azure_id"),
        mobilnummer = stringOrNull("mobilnummer"),
        epost = string("epost"),
        roller = roller,
        skalSlettesDato = localDateOrNull("skal_slettes_dato"),
    )
}

fun Session.createArrayOfRolle(
    values: Collection<Rolle>,
): Array = createArrayOf("rolle", values)
