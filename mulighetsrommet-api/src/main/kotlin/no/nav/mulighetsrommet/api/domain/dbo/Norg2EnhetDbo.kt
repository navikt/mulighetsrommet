package no.nav.mulighetsrommet.api.domain.dbo

import kotlinx.serialization.Serializable

@Serializable
data class Norg2EnhetDbo(
    val enhet_id: Int,
    val navn: String,
    val enhetNr: String,
    val status: EnhetStatus
)

@Serializable
enum class EnhetStatus() {
    UNDER_ETABLERING,
    AKTIV,
    UNDER_AVVIKLING,
    NEDLAGT
}
