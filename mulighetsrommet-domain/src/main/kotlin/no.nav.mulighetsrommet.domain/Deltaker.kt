package no.nav.mulighetsrommet.domain

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

enum class Deltakerstatus {
    IKKE_AKTUELL,
    VENTER,
    DELTAR,
    AVSLUTTET
}

@Serializable
data class Deltaker(
    val id: Int? = null,
    val tiltaksgjennomforingId: Int,
    val personId: Int,
    val status: Deltakerstatus
)
