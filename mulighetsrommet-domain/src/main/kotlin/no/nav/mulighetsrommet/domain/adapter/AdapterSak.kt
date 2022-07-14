package no.nav.mulighetsrommet.domain.adapter

import kotlinx.serialization.Serializable

@Serializable
data class AdapterSak(
    val id: Int,
    val lopenummer: Int,
    val aar: Int,
)
