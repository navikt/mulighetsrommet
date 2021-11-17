package no.nav.amt_informasjon_api.domain

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

@Serializable
data class Innsatsgruppe(
    val id: Int? = 0,
    val tittel: String,
    val beskrivelse: String,
)

object InnsatsgruppeTable : Table() {
    val id: Column<Int> = integer("id")
    val tittel: Column<String> = text("tittel")
    val beskrivelse: Column<String> = text("beskrivelse")

    override val primaryKey = PrimaryKey(id)
}
