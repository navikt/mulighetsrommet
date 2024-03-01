package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.dto.NavIdent

@Serializable
data class NavVeilederDto(
    val navIdent: NavIdent,
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
