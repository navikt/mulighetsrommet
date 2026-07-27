package no.nav.mulighetsrommet.admin.tiltak

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.tiltak.PrismodellType

@Serializable
data class PrismodellInfo(
    val type: PrismodellType,
    val navn: String,
    val beskrivelse: List<String>,
)
