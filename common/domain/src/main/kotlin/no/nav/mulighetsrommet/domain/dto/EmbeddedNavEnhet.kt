package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable

@Serializable
data class EmbeddedNavEnhet(
    val enhetsnummer: String,
    val navn: String,
    val type: NavEnhetType,
    val overordnetEnhet: String? = null,
)

@Serializable
enum class NavEnhetType {
    LOKAL,
    FYLKE,
    ALS,
    TILTAK,
    DIR,
}
