package no.nav.mulighetsrommet.api.domain.dbo

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type

@Serializable
data class NavEnhetDbo(
    val enhetId: Int,
    val navn: String,
    val enhetNr: String,
    val status: NavEnhetStatus,
    val type: Norg2Type,
    val overordnetEnhet: String?
)

@Serializable
enum class NavEnhetStatus {
    UNDER_ETABLERING,
    AKTIV,
    UNDER_AVVIKLING,
    NEDLAGT
}
