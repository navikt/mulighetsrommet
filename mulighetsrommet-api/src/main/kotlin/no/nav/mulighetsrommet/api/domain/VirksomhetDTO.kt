package no.nav.mulighetsrommet.api.domain

import kotlinx.serialization.Serializable

@Serializable
data class VirksomhetDTO(
    val organisasjonsnummer: String,
    val navn: String,
    val overordnetEnhet: String? = null
)
