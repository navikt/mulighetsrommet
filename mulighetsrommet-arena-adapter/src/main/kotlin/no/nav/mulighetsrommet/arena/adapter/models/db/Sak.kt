package no.nav.mulighetsrommet.arena.adapter.models.db

import kotlinx.serialization.Serializable

@Serializable
data class Sak(
    val sakId: Int,
    val lopenummer: Int,
    val aar: Int,
)
