package no.nav.amt_informasjon_api.domain

import kotlinx.serialization.Serializable
import no.nav.amt_informasjon_api.utils.DateSerializer
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.`java-time`.datetime
import java.time.LocalDateTime

@Serializable
data class Tiltaksgjennomforing(
    val id: Int? = 0,
    val tittel: String,
    val beskrivelse: String,
    val tiltaksvariantId: Int,
    val tiltaksnummer: Int,
    @Serializable(with = DateSerializer::class)
    val fraDato: LocalDateTime? = null,
    @Serializable(with = DateSerializer::class)
    val tilDato: LocalDateTime? = null
)

object TiltaksgjennomforingTable : IntIdTable() {
    val tiltaksvariantId = reference("tiltaksvariant_id", TiltaksvariantTable)
    val tittel: Column<String> = text("tittel")
    val beskrivelse: Column<String> = text("beskrivelse")
    val tiltaksnummer: Column<Int> = integer("tiltaksnummer")
    val fraDato: Column<LocalDateTime?> = datetime("fra_dato").nullable()
    val tilDato: Column<LocalDateTime?> = datetime("til_dato").nullable()
}
