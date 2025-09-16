package no.nav.mulighetsrommet.api.avtale.model

import kotlinx.serialization.Serializable

@Serializable
data class PrismodellInfo(
    val type: PrismodellType,
    val beskrivelse: String,
)
