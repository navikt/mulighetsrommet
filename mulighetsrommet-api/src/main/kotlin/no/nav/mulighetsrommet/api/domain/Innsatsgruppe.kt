package no.nav.mulighetsrommet.api.domain

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column

@Serializable
data class Innsatsgruppe(
    val id: Int? = 0,
    val tittel: String,
    val beskrivelse: String,
)

object InnsatsgruppeTable : IntIdTable() {
    val tittel: Column<String> = text("tittel")
    val beskrivelse: Column<String> = text("beskrivelse")
}
