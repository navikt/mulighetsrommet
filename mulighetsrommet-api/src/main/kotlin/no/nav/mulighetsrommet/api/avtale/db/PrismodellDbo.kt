package no.nav.mulighetsrommet.api.avtale.db

import no.nav.mulighetsrommet.api.avtale.model.AvtaltSats
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import java.util.UUID

data class PrismodellDbo(
    val id: UUID,
    val prismodellType: PrismodellType,
    val prisbetingelser: String?,
    val satser: List<AvtaltSats>,
)
