package no.nav.mulighetsrommet.api.services

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.database.Database
import no.nav.mulighetsrommet.api.utils.DatabaseUtils
import no.nav.mulighetsrommet.domain.Innsatsgruppe
import org.slf4j.Logger

class InnsatsgruppeService(private val db: Database, private val logger: Logger) {

    fun getInnsatsgrupper(): List<Innsatsgruppe> {
        val query = """
            select id, tittel, beskrivelse from innsatsgruppe
        """.trimIndent()
        val queryResult = queryOf(query).map { DatabaseUtils.toInnsatsgruppe(it) }.asList
        return db.session.run(queryResult)
    }
}
