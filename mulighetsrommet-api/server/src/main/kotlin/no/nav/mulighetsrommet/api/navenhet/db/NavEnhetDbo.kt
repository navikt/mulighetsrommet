package no.nav.mulighetsrommet.api.navenhet.db

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.model.NavEnhetNummer

@Serializable
data class NavEnhetDbo(
    val navn: String,
    val enhetsnummer: NavEnhetNummer,
    val status: NavEnhetStatus,
    val type: Norg2Type,
    val overordnetEnhet: NavEnhetNummer? = null,
)

@Serializable
enum class NavEnhetStatus {
    UNDER_ETABLERING,
    AKTIV,
    UNDER_AVVIKLING,
    NEDLAGT,
}
