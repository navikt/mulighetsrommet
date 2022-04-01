package no.nav.mulighetsrommet.api.domain

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column

object InnsatsgruppeTable : IntIdTable() {
    val tittel: Column<String> = text("tittel")
    val beskrivelse: Column<String> = text("beskrivelse")
}
