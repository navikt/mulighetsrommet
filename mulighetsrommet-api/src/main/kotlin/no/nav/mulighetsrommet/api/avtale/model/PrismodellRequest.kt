package no.nav.mulighetsrommet.api.avtale.model

import kotlinx.serialization.Serializable

@Serializable
data class PrismodellRequest(
    val type: Prismodell,
    val prisbetingelser: String?,
    val satser: List<AvtaltSatsDto>,
)
