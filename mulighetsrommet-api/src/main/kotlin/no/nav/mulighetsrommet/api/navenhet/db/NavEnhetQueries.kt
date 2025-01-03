package no.nav.mulighetsrommet.api.navenhet.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.database.createTextArray
import org.intellij.lang.annotations.Language

class NavEnhetQueries(private val session: Session) {

    fun upsert(enhet: NavEnhetDbo) = with(session) {
        @Language("PostgreSQL")
        val query = """
            insert into nav_enhet(navn, enhetsnummer, status, type, overordnet_enhet)
            values (:navn, :enhetsnummer, :status, :type, :overordnet_enhet)
            on conflict (enhetsnummer)
                do update set   navn            = excluded.navn,
                                enhetsnummer    = excluded.enhetsnummer,
                                status          = excluded.status,
                                type            = excluded.type,
                                overordnet_enhet = excluded.overordnet_enhet
        """.trimIndent()

        val params = mapOf(
            "navn" to enhet.navn,
            "enhetsnummer" to enhet.enhetsnummer,
            "status" to enhet.status.name,
            "type" to enhet.type.name,
            "overordnet_enhet" to enhet.overordnetEnhet,
        )

        execute(queryOf(query, params))
    }

    fun getAll(
        statuser: List<NavEnhetStatus>? = null,
        typer: List<Norg2Type>? = null,
        overordnetEnhet: String? = null,
    ): List<NavEnhetDbo> = with(session) {
        val parameters = mapOf(
            "statuser" to statuser?.map { it.name }?.let { createTextArray(it) },
            "typer" to typer?.map { it.name }?.let { createTextArray(it) },
            "overordnet_enhet" to overordnetEnhet,
        )

        @Language("PostgreSQL")
        val query = """
            select distinct e.navn, e.enhetsnummer, e.status, e.type, e.overordnet_enhet
            from nav_enhet e
            where (:statuser::text[] is null or e.status = any(:statuser))
              and (:typer::text[] is null or e.type = any(:typer))
              and (:overordnet_enhet::text is null or e.overordnet_enhet = :overordnet_enhet)
            order by e.navn
        """.trimIndent()

        return list(queryOf(query, parameters)) { it.toEnhetDbo() }
    }

    fun get(enhet: String): NavEnhetDbo? = with(session) {
        @Language("PostgreSQL")
        val query = """
            select navn, enhetsnummer, status, type, overordnet_enhet
            from nav_enhet
            where enhetsnummer = ?
        """.trimIndent()

        return single(queryOf(query, enhet)) { it.toEnhetDbo() }
    }

    fun deleteWhereEnhetsnummer(enhetsnummerForSletting: List<String>) = with(session) {
        val parameters = mapOf(
            "ider" to createTextArray(enhetsnummerForSletting),
        )

        @Language("PostgreSQL")
        val delete = """
            delete from nav_enhet where enhetsnummer = any(:ider::text[])
        """.trimIndent()

        execute(queryOf(delete, parameters))
    }

    fun getKostnadssted(regioner: List<String>): List<NavEnhetDbo> = with(session) {
        @Language("PostgreSQL")
        val query = """
            select
                nav_enhet.navn,
                nav_enhet.enhetsnummer,
                nav_enhet.status,
                nav_enhet.type,
                nav_enhet.overordnet_enhet
            from nav_enhet
                inner join kostnadssted on kostnadssted.enhetsnummer = nav_enhet.enhetsnummer
            where (:regioner::text[] is null or kostnadssted.region = any(:regioner))
        """.trimIndent()

        val params = mapOf(
            "regioner" to regioner.takeIf { it.isNotEmpty() }?.let { createTextArray(it) },
        )

        return list(queryOf(query, params)) { it.toEnhetDbo() }
    }
}

private fun Row.toEnhetDbo() = NavEnhetDbo(
    navn = string("navn"),
    enhetsnummer = string("enhetsnummer"),
    status = NavEnhetStatus.valueOf(string("status")),
    type = Norg2Type.valueOf(string("type")),
    overordnetEnhet = stringOrNull("overordnet_enhet"),
)
