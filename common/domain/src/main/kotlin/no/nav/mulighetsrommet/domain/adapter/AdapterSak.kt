package no.nav.mulighetsrommet.domain.adapter

import kotlinx.serialization.Serializable

@Serializable
data class AdapterSak(
    val sakId: Int,
    val lopenummer: Int,
    val aar: Int,
)
