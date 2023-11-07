package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type

@Serializable
data class EmbeddedNavEnhet(
    val enhetsnummer: String,
    val navn: String,
    val type: Norg2Type,
    val overordnetEnhet: String? = null,
)
