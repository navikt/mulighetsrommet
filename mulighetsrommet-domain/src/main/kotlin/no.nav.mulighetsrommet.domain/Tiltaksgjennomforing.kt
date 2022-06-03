package no.nav.mulighetsrommet.domain

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class Tiltaksgjennomforing(
    val id: Int? = null,
    val navn: String,
    val tiltakskode: String,
    val tiltaksnummer: Int,
    val arenaId: Int,
    val sakId: Int,
    val arrangorId: Int? = null,
    val sanityId: Int? = null,
    @Serializable(with = DateSerializer::class)
    val fraDato: LocalDateTime? = null,
    @Serializable(with = DateSerializer::class)
    val tilDato: LocalDateTime? = null
)
