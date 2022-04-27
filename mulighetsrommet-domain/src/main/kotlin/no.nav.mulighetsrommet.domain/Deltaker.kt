package no.nav.mulighetsrommet.domain

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

enum class Deltakerstatus {
    VENTER,
    DELTAR,
    AVSLUTTET
}

@Serializable
data class Deltaker(
    val id: Int? = null,
    val tiltaksgjennomforingId: Int,
    val personId: Int,
    @Serializable(with = DateSerializer::class)
    val fraDato: LocalDateTime? = null,
    @Serializable(with = DateSerializer::class)
    val tilDato: LocalDateTime? = null,
    val status: Deltakerstatus
)
