package no.nav.mulighetsrommet.api.services

import kotliquery.queryOf
import no.nav.mulighetsrommet.api.utils.DatabaseMapper
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.models.Innsatsgruppe

class InnsatsgruppeService(private val db: Database) {

    fun getInnsatsgrupper(): List<Innsatsgruppe> {
        val query = """
            select id, navn from innsatsgruppe
        """.trimIndent()
        val queryResult = queryOf(query).map { DatabaseMapper.toInnsatsgruppe(it) }.asList
        return db.run(queryResult)
    }
}
