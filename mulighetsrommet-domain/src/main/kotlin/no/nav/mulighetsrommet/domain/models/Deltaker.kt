package no.nav.mulighetsrommet.domain.models

import kotlinx.serialization.Serializable

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
