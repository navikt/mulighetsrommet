package no.nav.amt_informasjon_api.domain

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column

@Serializable
data class Tiltaksvariant(
    val id: Int? = 0,
    val tittel: String,
    val beskrivelse: String,
    val ingress: String,
)

object TiltaksvariantTable : IntIdTable() {
    val tittel: Column<String> = varchar("tittel", 50)
    val beskrivelse: Column<String> = text("beskrivelse")
    val ingress: Column<String> = varchar("ingress", 250)
    val archived: Column<Boolean> = bool("archived")
}
