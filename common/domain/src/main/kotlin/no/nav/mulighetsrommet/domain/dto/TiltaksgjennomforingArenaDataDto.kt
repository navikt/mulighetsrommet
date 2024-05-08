package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable

@Serializable
data class TiltaksgjennomforingArenaDataDto(
    val opprettetAar: Int,
    val lopenr: Int,
)
