package no.nav.tiltak.okonomi.oebs

import kotlinx.serialization.Serializable
import no.nav.tiltak.okonomi.api.serializers.OebsLocalDateTimeSerializer
import java.time.LocalDateTime


@Serializable
data class OebsBestillingKvittering(
    val bestillingsNummer: String,
    @Serializable(with = OebsLocalDateTimeSerializer::class)
    val opprettelsesTidspunkt: LocalDateTime,
    val statusOebs: String? = null,
    val feilMelding: String? = null,
    val feilKode: String? = null,
    val annullert: String? = null,
) {
    fun isSuccess(): Boolean = statusOebs != "Avvist" && feilKode == null && feilMelding == null
    fun isAnnulleringKvittering(): Boolean = annullert != null
}
