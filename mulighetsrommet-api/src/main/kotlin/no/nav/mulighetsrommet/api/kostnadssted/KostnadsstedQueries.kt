package no.nav.mulighetsrommet.api.kostnadssted

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.createTextArray
import no.nav.mulighetsrommet.model.NavEnhetNummer
import org.intellij.lang.annotations.Language

class KostnadsstedQueries(private val session: Session) {
    fun getAll(regioner: List<NavEnhetNummer> = listOf()): List<Kostnadssted> {
        @Language("PostgreSQL")
        val query = """
            select nav_enhet.enhetsnummer as kostnadssted_enhetsnummer,
                   nav_enhet.navn         as kostnadssted_navn,
                   region.enhetsnummer    as region_enhetsnummer,
                   region.navn            as region_navn
            from kostnadssted
                     join nav_enhet on kostnadssted.enhetsnummer = nav_enhet.enhetsnummer
                     join nav_enhet region on kostnadssted.region = region.enhetsnummer
            where (:regioner::text[] is null or kostnadssted.region = any(:regioner))
        """.trimIndent()

        val params = mapOf(
            "regioner" to regioner.ifEmpty { null }?.map { it.value }?.let { session.createTextArray(it) },
        )

        return session.list(queryOf(query, params)) { it.toKostnadssted() }
    }
}

private fun Row.toKostnadssted() = Kostnadssted(
    navn = string("kostnadssted_navn"),
    enhetsnummer = NavEnhetNummer(string("kostnadssted_enhetsnummer")),
    region = Kostnadssted.Region(
        navn = string("region_navn"),
        enhetsnummer = NavEnhetNummer(string("region_enhetsnummer")),
    ),
)
