package no.nav.mulighetsrommet.api.clients.brreg

import kotlinx.serialization.Serializable

@Serializable
data class BrregEnhetDto(
    val organisasjonsnummer: String,
    val navn: String,
    val overordnetEnhet: String? = null,
    val underenheter: List<BrregEnhetDto>? = null,
)

@Serializable
data class BrregEnhetUtenUnderenheterDto(
    val organisasjonsnummer: String,
    val navn: String,
)
