package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo

@Serializable
data class Kontorstruktur(
    val region: NavEnhetDbo,
    val kontorer: List<NavEnhetDbo>,
)
