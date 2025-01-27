package no.nav.mulighetsrommet.model

import kotlinx.serialization.Serializable

@Serializable
data class ArenaTiltaksgjennomforingDto(
    val arenaId: Int,
    val status: String,
)
