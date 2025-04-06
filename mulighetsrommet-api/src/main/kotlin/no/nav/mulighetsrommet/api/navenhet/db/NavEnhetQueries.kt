package no.nav.mulighetsrommet.api.navenhet.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.database.createArrayOfValue
import no.nav.mulighetsrommet.database.createTextArray
import no.nav.mulighetsrommet.model.NavEnhetNummer
import org.intellij.lang.annotations.Language

class NavEnhetQueries(private val session: Session) {

    fun upsert(enhet: NavEnhetDbo) {
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
            "enhetsnummer" to enhet.enhetsnummer.value,
            "status" to enhet.status.name,
            "type" to enhet.type.name,
            "overordnet_enhet" to enhet.overordnetEnhet?.value,
        )

        session.execute(queryOf(query, params))
    }

    fun getAll(
        statuser: List<NavEnhetStatus>? = null,
        typer: List<Norg2Type>? = null,
        overordnetEnhet: NavEnhetNummer? = null,
    ): List<NavEnhetDbo> {
        val parameters = mapOf(
            "statuser" to statuser?.let { session.createTextArray(it) },
            "typer" to typer?.let { session.createTextArray(it) },
            "overordnet_enhet" to overordnetEnhet?.value,
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

        return session.list(queryOf(query, parameters)) { it.toEnhetDbo() }
    }

    fun get(enhet: NavEnhetNummer): NavEnhetDbo? {
        @Language("PostgreSQL")
        val query = """
            select navn, enhetsnummer, status, type, overordnet_enhet
            from nav_enhet
            where enhetsnummer = ?
        """.trimIndent()

        return session.single(queryOf(query, enhet.value)) { it.toEnhetDbo() }
    }

    fun deleteWhereEnhetsnummer(enhetsnummerForSletting: List<NavEnhetNummer>) {
        val parameters = mapOf(
            "ider" to session.createArrayOfValue(enhetsnummerForSletting) { it.value },
        )

        @Language("PostgreSQL")
        val delete = """
            delete from nav_enhet where enhetsnummer = any(:ider::text[])
        """.trimIndent()

        session.execute(queryOf(delete, parameters))
    }

    fun getKostnadssted(regioner: List<NavEnhetNummer>): List<NavEnhetDbo> {
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
            "regioner" to regioner.takeIf { it.isNotEmpty() }?.map { it.value }?.let { session.createTextArray(it) },
        )

        return session.list(queryOf(query, params)) { it.toEnhetDbo() }
    }
}

private fun Row.toEnhetDbo() = NavEnhetDbo(
    navn = string("navn"),
    enhetsnummer = NavEnhetNummer(string("enhetsnummer")),
    status = NavEnhetStatus.valueOf(string("status")),
    type = Norg2Type.valueOf(string("type")),
    overordnetEnhet = stringOrNull("overordnet_enhet")?.let { NavEnhetNummer(it) },
)
