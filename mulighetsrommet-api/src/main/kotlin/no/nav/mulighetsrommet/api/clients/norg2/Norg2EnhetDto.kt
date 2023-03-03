package no.nav.mulighetsrommet.api.clients.norg2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Norg2EnhetDto(
    val enhetId: Int,
    val navn: String,
    val enhetNr: String,
    val status: Norg2EnhetStatus
)

@Serializable
enum class Norg2EnhetStatus {
    @SerialName("Under etablering")
    UNDER_ETABLERING,

    @SerialName("Aktiv")
    AKTIV,

    @SerialName("Under avvikling")
    UNDER_AVVIKLING,

    @SerialName("Nedlagt")
    NEDLAGT
}
