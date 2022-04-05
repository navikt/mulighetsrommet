package no.nav.mulighetsrommet.domain

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class Tiltaksgjennomforing(
    val id: Int? = 0,
    val tittel: String,
    val beskrivelse: String,
    val tiltakskode: Tiltakskode,
    val tiltaksnummer: Int,
    @Serializable(with = DateSerializer::class)
    val fraDato: LocalDateTime,
    @Serializable(with = DateSerializer::class)
    val tilDato: LocalDateTime
)
