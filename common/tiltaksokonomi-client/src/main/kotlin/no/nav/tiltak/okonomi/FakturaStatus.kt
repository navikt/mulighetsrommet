package no.nav.tiltak.okonomi

import kotlinx.serialization.Serializable

@Serializable
data class FakturaStatus(
    val fakturanummer: String,
    val status: FakturaStatusType,
)

enum class FakturaStatusType {
    SENDT,
    UTBETALT,
}
