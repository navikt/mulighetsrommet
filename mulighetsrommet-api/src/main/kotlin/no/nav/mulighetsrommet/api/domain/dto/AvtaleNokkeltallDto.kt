package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.Serializable

@Serializable
data class AvtaleNokkeltallDto(
    val antallTiltaksgjennomforinger: Int,
    val antallDeltakere: Int
)
