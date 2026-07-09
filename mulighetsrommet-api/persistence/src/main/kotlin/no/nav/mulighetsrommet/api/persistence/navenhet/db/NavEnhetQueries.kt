package no.nav.mulighetsrommet.api.persistence.navenhet.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhet
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetRepository
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetStatus
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetType
import no.nav.mulighetsrommet.database.createTextArray
import no.nav.mulighetsrommet.model.NavEnhetNummer
import org.intellij.lang.annotations.Language

class NavEnhetQueries(private val session: Session) : NavEnhetRepository {

    override fun save(navEnhet: NavEnhet) {
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
            "navn" to navEnhet.navn,
            "enhetsnummer" to navEnhet.enhetsnummer.value,
            "status" to navEnhet.status.name,
            "type" to navEnhet.type.name,
            "overordnet_enhet" to navEnhet.overordnetEnhet?.value,
        )

        session.execute(queryOf(query, params))
    }

    override fun getAll(
        statuser: List<NavEnhetStatus>?,
        typer: List<NavEnhetType>?,
        overordnetEnhet: NavEnhetNummer?,
    ): List<NavEnhet> {
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

        return session.list(queryOf(query, parameters)) { it.toNavEnhet() }
    }

    override fun get(enhetsnummer: NavEnhetNummer): NavEnhet? {
        @Language("PostgreSQL")
        val query = """
            select navn, enhetsnummer, status, type, overordnet_enhet
            from nav_enhet
            where enhetsnummer = ?
        """.trimIndent()

        return session.single(queryOf(query, enhetsnummer.value)) { it.toNavEnhet() }
    }
}

private fun Row.toNavEnhet() = NavEnhet(
    navn = string("navn"),
    enhetsnummer = NavEnhetNummer(string("enhetsnummer")),
    status = NavEnhetStatus.valueOf(string("status")),
    type = NavEnhetType.valueOf(string("type")),
    overordnetEnhet = stringOrNull("overordnet_enhet")?.let { NavEnhetNummer(it) },
)
