package no.nav.mulighetsrommet.kafka.domain

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.kafka.utils.DateSerializer
import java.time.LocalDateTime

@Serializable
data class Tiltaksgjennomforing(
    val id: Int? = 0,
    val tittel: String,
    val beskrivelse: String,
    val tiltakskode: Tiltakskode,
    val tiltaksnummer: Int,
    @Serializable(with = DateSerializer::class)
    val fraDato: LocalDateTime? = null,
    @Serializable(with = DateSerializer::class)
    val tilDato: LocalDateTime? = null
)
