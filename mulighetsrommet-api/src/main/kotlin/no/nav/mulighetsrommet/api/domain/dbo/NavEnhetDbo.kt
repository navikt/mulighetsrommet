package no.nav.mulighetsrommet.api.domain.dbo

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type

@Serializable
data class NavEnhetDbo(
    val navn: String,
    val enhetsnummer: String,
    val status: NavEnhetStatus,
    val type: Norg2Type,
    val overordnetEnhet: String? = null,
)

@Serializable
enum class NavEnhetStatus {
    UNDER_ETABLERING,
    AKTIV,
    UNDER_AVVIKLING,
    NEDLAGT,
}
