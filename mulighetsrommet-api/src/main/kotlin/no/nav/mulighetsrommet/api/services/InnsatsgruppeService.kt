package no.nav.mulighetsrommet.api.services

import kotliquery.queryOf
import no.nav.mulighetsrommet.api.database.Database
import no.nav.mulighetsrommet.api.utils.DatabaseMapper
import no.nav.mulighetsrommet.domain.Innsatsgruppe
import org.slf4j.Logger

class InnsatsgruppeService(private val db: Database, private val logger: Logger) {

    fun getInnsatsgrupper(): List<Innsatsgruppe> {
        val query = """
            select id, navn from innsatsgruppe
        """.trimIndent()
        val queryResult = queryOf(query).map { DatabaseMapper.toInnsatsgruppe(it) }.asList
        return db.session.run(queryResult)
    }
}
