package no.nav.tiltak.okonomi

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.InstantSerializer
import java.time.Instant

@Serializable
data class BestillingStatus(
    val bestillingsnummer: Bestillingsnummer,
    val status: BestillingStatusType,
    @Serializable(with = InstantSerializer::class)
    val statusSistOppdatert: Instant,
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
     * Sendt annullering til OeBS
     */
    ANNULLERING_SENDT,

    /**
     * Mottatt kvittering på annullering fra OeBS
     */
    ANNULLERT,

    /**
     * Bestillingen er markert som oppgjort
     */
    OPPGJORT,

    /**
     * Krever manuell oppfølging
     */
    FEILET,
}
