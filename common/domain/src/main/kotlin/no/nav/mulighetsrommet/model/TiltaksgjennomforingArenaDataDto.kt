package no.nav.mulighetsrommet.model

import kotlinx.serialization.Serializable

@Serializable
data class TiltaksgjennomforingArenaDataDto(
    val opprettetAar: Int,
    val lopenr: Int,
)
