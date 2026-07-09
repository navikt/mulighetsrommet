package no.nav.mulighetsrommet.api.veilederflate.models

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhet
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetType
import no.nav.mulighetsrommet.model.NavEnhetNummer

@Serializable
data class NavEnhetDto(
    val navn: String,
    val enhetsnummer: NavEnhetNummer,
    val type: NavEnhetType,
    val overordnetEnhet: NavEnhetNummer?,
)

fun NavEnhet.toDto() = NavEnhetDto(
    navn = navn,
    enhetsnummer = enhetsnummer,
    type = type,
    overordnetEnhet = overordnetEnhet,
)
