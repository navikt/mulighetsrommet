package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable

@Serializable
data class ArenaTiltaksgjennomforingDto(
    val arenaId: Int,
    val status: String,
)
