package no.nav.mulighetsrommet.api.services

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.database.Database
import no.nav.mulighetsrommet.domain.Innsatsgruppe

class InnsatsgruppeService(private val db: Database) {

    fun getInnsatsgrupper(): List<Innsatsgruppe> {
        val query = """
            select id, tittel, beskrivelse from innsatsgruppe
        """.trimIndent()
        val queryResult = queryOf(query).map {toInnsatsgruppe(it)}.asList
        return db.session.run(queryResult)
    }

    private fun toInnsatsgruppe(row: Row): Innsatsgruppe = Innsatsgruppe(
        id = row.int("id"),
        tittel = row.string("tittel"),
        beskrivelse = row.string("beskrivelse")
    )
}
