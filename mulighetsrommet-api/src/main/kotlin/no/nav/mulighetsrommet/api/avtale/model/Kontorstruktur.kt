package no.nav.mulighetsrommet.api.avtale.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetDbo

@Serializable
data class Kontorstruktur(
    val region: NavEnhetDbo,
    val kontorer: List<NavEnhetDbo>,
)
