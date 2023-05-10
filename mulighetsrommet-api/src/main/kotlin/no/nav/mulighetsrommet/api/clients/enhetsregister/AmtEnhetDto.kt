package no.nav.mulighetsrommet.api.clients.enhetsregister

import kotlinx.serialization.Serializable

@Serializable
internal data class AmtEnhetDto(
    val organisasjonsnummer: String,
    val navn: String,
    val overordnetEnhetOrganisasjonsnummer: String? = null,
    val overordnetEnhetNavn: String? = null,
)
