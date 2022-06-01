package no.nav.mulighetsrommet.domain

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class Tiltaksgjennomforing(
    val id: Int? = null,
    val navn: String,
    val tiltakskode: String,
    val tiltaksnummer: Int,
    @Serializable(with = DateSerializer::class)
    val oppstart: LocalDateTime? = null,
)
