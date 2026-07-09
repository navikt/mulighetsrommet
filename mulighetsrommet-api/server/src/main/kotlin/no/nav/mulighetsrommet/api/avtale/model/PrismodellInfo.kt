package no.nav.mulighetsrommet.api.avtale.model

import kotlinx.serialization.Serializable

@Serializable
data class PrismodellInfo(
    val type: PrismodellType,
    val navn: String,
    val beskrivelse: List<String>,
)
