package no.nav.mulighetsrommet.api.domain

import no.nav.mulighetsrommet.api.database.PGEnum
import no.nav.mulighetsrommet.domain.Tiltakskode
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.`java-time`.datetime
import java.time.LocalDateTime

object TiltaksgjennomforingTable : IntIdTable() {
    val tiltakskode: Column<Tiltakskode> = customEnumeration("tiltakskode", "tiltakskode", { value -> Tiltakskode.valueOf(value as String) }, { PGEnum("tiltakskode", it) }).references(TiltakstypeTable.tiltakskode)
    val tittel: Column<String> = text("tittel")
    val beskrivelse: Column<String> = text("beskrivelse")
    val tiltaksnummer: Column<Int> = integer("tiltaksnummer")
    val fraDato: Column<LocalDateTime?> = datetime("fra_dato").nullable()
    val tilDato: Column<LocalDateTime?> = datetime("til_dato").nullable()
}
