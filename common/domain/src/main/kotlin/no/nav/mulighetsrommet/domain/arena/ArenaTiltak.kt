package no.nav.mulighetsrommet.domain.arena

import kotlinx.serialization.Serializable

@Serializable
data class ArenaTiltak (
    val TILTAKSNAVN: String,
    val TILTAKSKODE: String,
    val DATO_FRA: String?,
    val DATO_TIL: String?,
)
