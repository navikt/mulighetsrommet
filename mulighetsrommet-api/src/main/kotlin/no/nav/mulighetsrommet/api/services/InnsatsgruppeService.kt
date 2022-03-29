package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.database.DatabaseFactory
import no.nav.mulighetsrommet.api.domain.Innsatsgruppe
import no.nav.mulighetsrommet.api.domain.InnsatsgruppeTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll

class InnsatsgruppeService(private val db: DatabaseFactory) {

    suspend fun getInnsatsgrupper(): List<Innsatsgruppe> {
        return db.dbQuery {
            InnsatsgruppeTable
                .selectAll()
                .toList()
                .map { toInnsatsgruppe(it) }
        }
    }

    private fun toInnsatsgruppe(row: ResultRow): Innsatsgruppe = Innsatsgruppe(
        tittel = row[InnsatsgruppeTable.tittel],
        beskrivelse = row[InnsatsgruppeTable.beskrivelse],
    )
}
