package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.Serializable

@Serializable
data class TiltaksgjennomforingNokkeltallDto(
    val antallDeltakere: Int,
)
