package no.nav.mulighetsrommet.api.avtale.db

import no.nav.mulighetsrommet.api.avtale.model.AvtaltSats
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import java.util.*

data class PrismodellDbo(
    val prismodell: Prismodell,
    val prisbetingelser: String?,
    val satser: List<AvtaltSats>,
)
