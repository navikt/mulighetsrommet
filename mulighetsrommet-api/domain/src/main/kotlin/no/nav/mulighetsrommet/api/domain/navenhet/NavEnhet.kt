package no.nav.mulighetsrommet.api.domain.navenhet

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.NavEnhetNummer

@Serializable
data class NavEnhet(
    val enhetsnummer: NavEnhetNummer,
    val navn: String,
    val status: NavEnhetStatus,
    val type: NavEnhetType,
    val overordnetEnhet: NavEnhetNummer? = null,
)
