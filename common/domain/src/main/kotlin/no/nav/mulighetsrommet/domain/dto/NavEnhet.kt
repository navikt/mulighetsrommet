package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable

@Serializable
data class NavEnhet(
    val enhetsnummer: String,
    val navn: String? = null,
)
