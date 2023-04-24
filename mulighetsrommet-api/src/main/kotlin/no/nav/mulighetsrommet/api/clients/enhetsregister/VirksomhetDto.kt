package no.nav.mulighetsrommet.api.clients.enhetsregister

import kotlinx.serialization.Serializable

@Serializable
data class VirksomhetDto(
    val organisasjonsnummer: String,
    val navn: String,
    val overordnetEnhetOrganisasjonsnummer: String? = null,
    val overordnetEnhetNavn: String? = null,
)
