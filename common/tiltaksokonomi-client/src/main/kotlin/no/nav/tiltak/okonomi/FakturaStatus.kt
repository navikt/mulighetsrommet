package no.nav.tiltak.okonomi

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import java.time.LocalDateTime

@Serializable
data class FakturaStatus(
    val fakturanummer: String,
    val status: FakturaStatusType,
    @Serializable(with = LocalDateTimeSerializer::class)
    val fakturaStatusSistOppdatert: LocalDateTime?,
)

enum class FakturaStatusType {
    /**
     * Sendt til OeBS, ikke mottatt kvittering enda
     */
    SENDT,

    /**
     * Fakturaen er prosessert ok i OeBS, men ikke sendt til banken
     */
    IKKE_BETALT,

    /**
     * Noe av beløpet er sendt til banken (visstnok lite brukt)
     */
    DELVIS_BETALT,

    /**
     * Betyr at hele beløpet er sendt til banken
     */
    FULLT_BETALT,

    /**
     * Krever manuell oppfølging
     */
    FEILET,
}
