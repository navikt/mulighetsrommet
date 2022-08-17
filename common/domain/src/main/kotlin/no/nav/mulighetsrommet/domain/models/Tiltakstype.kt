package no.nav.mulighetsrommet.domain.models

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class Tiltakstype(
    val id: Int? = null,
    val navn: String,
    val innsatsgruppe: Int,
    val tiltakskode: String,
)
