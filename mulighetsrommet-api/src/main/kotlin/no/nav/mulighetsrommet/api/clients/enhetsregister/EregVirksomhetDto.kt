package no.nav.mulighetsrommet.api.clients.enhetsregister

import kotlinx.serialization.Serializable

@Serializable
data class EregVirksomhetDto(
    val organisasjonsnummer: String,
    val navn: Navn,
    val organisasjonsleddOver: OrganisasjonsleddOver? = null,
)

@Serializable
data class Navn(
    val navnelinje1: String,
)

@Serializable
data class OrganisasjonsleddOver(
    val organisasjonsledd: Organisasjonsledd,
)

@Serializable
data class Organisasjonsledd(
    val organisasjonsnummer: String,
    val navn: Navn,
)
