package no.nav.mulighetsrommet.domain.arena

import kotlinx.serialization.Serializable

@Serializable
data class ArenaSak (
    val SAK_ID: Int,
    val SAKSKODE: String,
    val AAR: Int,
    val LOPENRSAK: Int,
)
