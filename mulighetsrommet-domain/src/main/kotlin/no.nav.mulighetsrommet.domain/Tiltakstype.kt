package no.nav.mulighetsrommet.domain

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class Tiltakstype(
    val id: Int? = null,
    val navn: String,
    val innsatsgruppe: Int,
    val sanityId: Int? = null,
    val tiltakskode: String,
    @Serializable(with = DateSerializer::class)
    val fraDato: LocalDateTime? = null,
    @Serializable(with = DateSerializer::class)
    val tilDato: LocalDateTime? = null,
)
