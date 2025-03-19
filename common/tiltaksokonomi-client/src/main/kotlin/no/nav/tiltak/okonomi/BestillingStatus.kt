package no.nav.tiltak.okonomi

import kotlinx.serialization.Serializable

@Serializable
data class BestillingStatus(
    val bestillingsnummer: String,
    val status: BestillingStatusType,
)

enum class BestillingStatusType {
    SENDT,
    AKTIV,
    ANNULLERT,
    FRIGJORT,
}
