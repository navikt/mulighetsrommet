package no.nav.tiltak.okonomi.oebs

import kotlinx.serialization.Serializable
import no.nav.tiltak.okonomi.FakturaStatusType
import no.nav.tiltak.okonomi.api.serializers.OebsLocalDateTimeSerializer
import java.time.LocalDateTime

@Serializable
data class OebsFakturaKvittering(
    val fakturaNummer: String,
    @Serializable(with = OebsLocalDateTimeSerializer::class)
    val opprettelsesTidspunkt: LocalDateTime,
    val statusOpprettet: String? = null,
    val statusBetalt: StatusBetalt? = null,
    val feilMelding: String? = null,
    val feilKode: String? = null,
) {
    fun isSuccess(): Boolean = statusOpprettet != "Avvist" && feilKode == null && feilMelding == null

    enum class StatusBetalt {
        /**
         * Prosessert ok i OeBS, men at den ikke er sendt til banken
         */
        IkkeBetalt,

        /**
         * Betyr at noe av beløpet er sendt til banken (visstnok lite brukt)
         */
        DelvisBetalt,

        /**
         * Betyr at hele beløpet er sendt til banken
         */
        FulltBetalt,
        ;

        fun toFakturaStatusType(): FakturaStatusType = when (this) {
            IkkeBetalt -> FakturaStatusType.IKKE_BETALT
            DelvisBetalt -> FakturaStatusType.DELVIS_BETALT
            FulltBetalt -> FakturaStatusType.FULLT_BETALT
        }
    }
}
