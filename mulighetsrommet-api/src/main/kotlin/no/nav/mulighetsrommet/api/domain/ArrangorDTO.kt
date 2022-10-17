package no.nav.mulighetsrommet.api.domain

import kotlinx.serialization.Serializable

@Serializable
data class ArrangorDTO(
    val virksomhetsnummer: String,
    val organisasjonsnummerMorselskap: String
)
