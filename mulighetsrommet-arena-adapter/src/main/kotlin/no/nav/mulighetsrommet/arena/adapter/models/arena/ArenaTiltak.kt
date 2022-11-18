package no.nav.mulighetsrommet.arena.adapter.models.arena

import kotlinx.serialization.Serializable

@Serializable
data class ArenaTiltak(
    val TILTAKSNAVN: String,
    val TILTAKSKODE: String,
    val DATO_FRA: String?,
    val DATO_TIL: String?,
)
