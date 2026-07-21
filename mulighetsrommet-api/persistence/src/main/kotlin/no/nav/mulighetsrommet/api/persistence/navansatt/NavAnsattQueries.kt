package no.nav.mulighetsrommet.api.persistence.navansatt

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsatt
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsattRepository
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsattRolle
import no.nav.mulighetsrommet.database.createArrayOfValue
import no.nav.mulighetsrommet.database.requireSingle
import no.nav.mulighetsrommet.database.withTransaction
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import org.intellij.lang.annotations.Language
import java.util.UUID

class NavAnsattQueries(private val session: Session) : NavAnsattRepository {

    override fun save(ansatt: NavAnsatt) = withTransaction(session) {
        upsertAnsatt(ansatt)
        setRoller(ansatt.navIdent, ansatt.roller)
    }

    context(session: TransactionalSession)
    private fun upsertAnsatt(ansatt: NavAnsatt) {
        @Language("PostgreSQL")
        val query = """
            insert into nav_ansatt(nav_ident, fornavn, etternavn, hovedenhet, entra_object_id, mobilnummer, epost, skal_slettes_dato)
            values (:nav_ident, :fornavn, :etternavn, :hovedenhet, :entra_object_id::uuid, :mobilnummer, :epost, :skal_slettes_dato)
            on conflict (nav_ident)
                do update set fornavn           = excluded.fornavn,
                              etternavn         = excluded.etternavn,
                              hovedenhet        = excluded.hovedenhet,
                              entra_object_id   = excluded.entra_object_id,
                              mobilnummer       = excluded.mobilnummer,
                              epost             = excluded.epost,
                              skal_slettes_dato = excluded.skal_slettes_dato
        """.trimIndent()
        val params = mapOf(
            "nav_ident" to ansatt.navIdent.value,
            "fornavn" to ansatt.fornavn,
            "etternavn" to ansatt.etternavn,
            "hovedenhet" to ansatt.hovedenhet.value,
            "entra_object_id" to ansatt.entraObjectId,
            "mobilnummer" to ansatt.mobilnummer,
            "epost" to ansatt.epost,
            "skal_slettes_dato" to ansatt.skalSlettesDato,
        )
        session.execute(queryOf(query, params))
    }

    context(session: TransactionalSession)
    private fun setRoller(navIdent: NavIdent, roller: Set<NavAnsattRolle>) {
        @Language("PostgreSQL")
        val deleteRoles = """
            delete from nav_ansatt_rolle
            where nav_ansatt_nav_ident = ?
              and not (rolle = any(?::text[]))
        """.trimIndent()
        session.execute(
            queryOf(
                deleteRoles,
                navIdent.value,
                session.createArrayOfValue(roller) { it.rolle },
            ),
        )

        if (roller.isNotEmpty()) {
            @Language("PostgreSQL")
            val insertRolle = """
                insert into nav_ansatt_rolle(nav_ansatt_nav_ident, generell, rolle)
                values (:nav_ident, :generell, :rolle)
                on conflict (nav_ansatt_nav_ident, rolle) do update set
                    generell = excluded.generell
            """.trimIndent()

            @Language("PostgreSQL")
            val selectRolleId = """
                select id
                from nav_ansatt_rolle
                where nav_ansatt_nav_ident = :nav_ident
                  and rolle = :rolle;
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
                val paramsRolle = mapOf(
                    "nav_ident" to navIdent.value,
                    "generell" to rolle.generell,
                    "rolle" to rolle.rolle.name,
                )
                session.execute(queryOf(insertRolle, paramsRolle))

                val id = session.requireSingle(queryOf(selectRolleId, paramsRolle)) { it.int("id") }
                session.execute(
                    queryOf(deleteEnheter, id, session.createArrayOfValue(rolle.enheter) { it.value }),
                )

                val paramsRolleEnheter = rolle.enheter.map { mapOf("role_id" to id, "enhet" to it.value) }
                session.batchPreparedNamedStatement(insertRolleEnhet, paramsRolleEnheter)
            }
        }
    }

    override fun getAll(): List<NavAnsatt> = with(session) {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_nav_ansatt
            order by fornavn, etternavn
        """.trimIndent()

        return list(queryOf(query)) { it.toNavAnsatt() }
    }

    override fun getOrError(navIdent: NavIdent): NavAnsatt {
        return checkNotNull(get(navIdent)) { "NavAnsatt ikke funnet" }
    }

    override fun get(navIdent: NavIdent): NavAnsatt? = with(session) {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_nav_ansatt
            where nav_ident = ?
        """.trimIndent()

        return single(queryOf(query, navIdent.value)) { it.toNavAnsatt() }
    }

    override fun getByEntraObjectId(objectId: UUID): NavAnsatt? = with(session) {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_nav_ansatt
            where entra_object_id = ?::uuid
        """.trimIndent()

        return single(queryOf(query, objectId)) { it.toNavAnsatt() }
    }

    override fun deleteByEntraObjectId(objectId: UUID): Int = with(session) {
        @Language("PostgreSQL")
        val query = """
            delete from nav_ansatt
            where entra_object_id = ?::uuid
        """.trimIndent()

        return update(queryOf(query, objectId))
    }
}

private fun Row.toNavAnsatt(): NavAnsatt {
    val roller = stringOrNull("roller_json")
        ?.let { Json.decodeFromString<Set<NavAnsattRolle>>(it) }
        ?: setOf()
    return NavAnsatt.fromStorage(
        navIdent = NavIdent(string("nav_ident")),
        fornavn = string("fornavn"),
        etternavn = string("etternavn"),
        hovedenhet = NavEnhetNummer(string("hovedenhet_enhetsnummer")),
        entraObjectId = uuid("entra_object_id"),
        mobilnummer = stringOrNull("mobilnummer"),
        epost = string("epost"),
        roller = roller,
        skalSlettesDato = localDateOrNull("skal_slettes_dato"),
    )
}
