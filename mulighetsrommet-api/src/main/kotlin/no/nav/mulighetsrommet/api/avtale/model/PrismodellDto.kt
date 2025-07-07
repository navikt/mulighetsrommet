package no.nav.mulighetsrommet.api.avtale.model

import kotlinx.serialization.Serializable

@Serializable
data class PrismodellDto(
    val type: Prismodell,
    val beskrivelse: String,
)
