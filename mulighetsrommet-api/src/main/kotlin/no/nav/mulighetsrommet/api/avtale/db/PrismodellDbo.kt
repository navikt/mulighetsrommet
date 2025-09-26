package no.nav.mulighetsrommet.api.avtale.db

import no.nav.mulighetsrommet.api.avtale.model.AvtaltSats
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType

data class PrismodellDbo(
    val prismodell: PrismodellType,
    val prisbetingelser: String?,
    val satser: List<AvtaltSats>,
)
