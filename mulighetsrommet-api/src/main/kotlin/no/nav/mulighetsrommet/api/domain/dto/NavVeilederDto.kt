package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.Serializable

@Serializable
data class NavVeilederDto(
    val navIdent: String,
    val fornavn: String,
    val etternavn: String,
    val hovedenhet: Hovedenhet,
) {
    @Serializable
    data class Hovedenhet(
        val enhetsnummer: String,
        val navn: String,
    )
}
