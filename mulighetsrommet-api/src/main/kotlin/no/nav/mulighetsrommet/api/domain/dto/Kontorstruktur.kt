package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.Serializable

@Serializable
data class Kontorstruktur(
    val region: EmbeddedNavEnhet,
    val kontorer: List<EmbeddedNavEnhet>,
)
