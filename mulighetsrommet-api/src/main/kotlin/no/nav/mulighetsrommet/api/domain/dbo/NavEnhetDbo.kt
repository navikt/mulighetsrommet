package no.nav.mulighetsrommet.api.domain.dbo

import kotlinx.serialization.Serializable

@Serializable
data class NavEnhetDbo(
    val enhetId: Int,
    val navn: String,
    val enhetNr: String,
    val status: NavEnhetStatus
)

@Serializable
enum class NavEnhetStatus {
    UNDER_ETABLERING,
    AKTIV,
    UNDER_AVVIKLING,
    NEDLAGT
}
