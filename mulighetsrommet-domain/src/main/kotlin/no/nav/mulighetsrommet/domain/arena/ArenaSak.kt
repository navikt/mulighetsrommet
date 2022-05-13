package no.nav.mulighetsrommet.domain.arena

import kotlinx.serialization.Serializable

@Serializable
data class ArenaSak(
    val sakId: Int,
    val lopenrsak: Int,
    val aar: Int,
    val aetatenhetAnsvarlig: Int
)
