package no.nav.tiltak.okonomi

import kotlinx.serialization.Serializable

@Serializable
data class BestillingStatus(
    val bestillingsnummer: String,
    val status: BestillingStatusType,
)

enum class BestillingStatusType {
    /**
     * Sendt til OeBS, venter på kvittering
     */
    SENDT,

    /**
     * OK kvittering fra OeBS
     */
    AKTIV,

    /**
     * Mottatt kvittering på annulering fra OeBS
     */
    ANNULLERT,

    /**
     * Sendt annulering til OeBS
     */
    ANNULLERING_SENDT,
    OPPGJORT,

    /**
     * Krever manuell oppfølging
     */
    FEILET,
}
