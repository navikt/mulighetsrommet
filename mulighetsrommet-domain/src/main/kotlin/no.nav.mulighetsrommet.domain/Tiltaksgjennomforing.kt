package no.nav.mulighetsrommet.domain

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class Tiltaksgjennomforing(
    val id: Int? = null,
    val navn: String,
    val arrangorId: Int,
    val tiltakskode: Tiltakskode,
    val tiltaksnummer: Int,
    val arenaId: Int,
    val sakId: Int,
    val sanityId: Int? = null,
    @Serializable(with = DateSerializer::class)
    val fraDato: LocalDateTime,
    @Serializable(with = DateSerializer::class)
    val tilDato: LocalDateTime
)
