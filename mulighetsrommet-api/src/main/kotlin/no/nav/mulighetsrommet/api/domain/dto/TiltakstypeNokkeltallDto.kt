package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.Serializable

@Serializable
data class TiltakstypeNokkeltallDto(
    val antallTiltaksgjennomforinger: Int,
    val antallAvtaler: Int,
    val antallDeltakere: Int
)
