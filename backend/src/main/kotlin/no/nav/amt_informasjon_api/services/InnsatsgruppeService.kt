package no.nav.amt_informasjon_api.services

import no.nav.amt_informasjon_api.database.DatabaseFactory
import no.nav.amt_informasjon_api.domain.Innsatsgruppe
import no.nav.amt_informasjon_api.domain.InnsatsgruppeTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll

class InnsatsgruppeService {

    suspend fun getInnsatsgrupper(): List<Innsatsgruppe> {
        return DatabaseFactory.dbQuery {
            InnsatsgruppeTable
                .selectAll()
                .toList()
                .map { toInnsatsgruppe(it) }
        }
    }

    private fun toInnsatsgruppe(row: ResultRow): Innsatsgruppe = Innsatsgruppe(
        id = row[InnsatsgruppeTable.id],
        tittel = row[InnsatsgruppeTable.tittel],
        beskrivelse = row[InnsatsgruppeTable.beskrivelse],
    )
}
