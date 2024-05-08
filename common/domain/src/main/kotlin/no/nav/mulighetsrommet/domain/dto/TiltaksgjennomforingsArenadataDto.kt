package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable

@Serializable
data class TiltaksgjennomforingsArenadataDto(
    val opprettetAar: Int,
    val lopenr: Int,
)
