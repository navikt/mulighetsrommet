package no.nav.mulighetsrommet.api.domain

import no.nav.mulighetsrommet.api.database.PGEnum
import no.nav.mulighetsrommet.domain.Tiltakskode
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.`java-time`.datetime
import java.time.LocalDateTime

object TiltakstypeTable : IntIdTable() {
    val navn: Column<String> = text("navn")
    val innsatsgruppeId: Column<Int> = integer("innsatsgruppe_id").references(InnsatsgruppeTable.id)
    val sanityId: Column<Int?> = integer("sanity_id").nullable()
    val tiltakskode: Column<Tiltakskode> = customEnumeration("tiltakskode", "tiltakskode", { value -> Tiltakskode.valueOf(value as String) }, { PGEnum("tiltakskode", it) })
    val fraDato: Column<LocalDateTime?> = datetime("dato_fra").nullable()
    val tilDato: Column<LocalDateTime?> = datetime("dato_til").nullable()
    val createdBy: Column<String?> = text("created_by").nullable()
    val createdAt: Column<LocalDateTime> = datetime("created_at")
    val updatedBy: Column<String?> = text("updated_by").nullable()
    val updatedAt: Column<LocalDateTime> = datetime("updated_at")
}
