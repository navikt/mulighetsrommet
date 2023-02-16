package no.nav.mulighetsrommet.api.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Norg2Enhet(
    val enhetId: Int,
    val navn: String,
    val enhetNr: String,
    val status: EnhetStatus
)

@Serializable
enum class EnhetStatus() {
    @SerialName("Under etablering")
    UNDER_ETABLERING,

    @SerialName("Aktiv")
    AKTIV,

    @SerialName("Under avvikling")
    UNDER_AVVIKLING,

    @SerialName("Nedlagt")
    NEDLAGT
}
