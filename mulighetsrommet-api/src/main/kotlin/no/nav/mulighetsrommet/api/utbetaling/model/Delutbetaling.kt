package no.nav.mulighetsrommet.api.utbetaling.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.serializers.InstantSerializer
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.tiltak.okonomi.FakturaStatusType
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

@Serializable
data class Delutbetaling(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val tilsagnId: UUID,
    @Serializable(with = UUIDSerializer::class)
    val utbetalingId: UUID,
    val status: DelutbetalingStatus,
    val periode: Periode,
    val belop: Int,
    val gjorOppTilsagn: Boolean,
    val lopenummer: Int,
    val faktura: Faktura,
) {
    @Serializable
    data class Faktura(
        val fakturanummer: String,
        @Serializable(with = LocalDateTimeSerializer::class)
        val sendtTidspunkt: LocalDateTime?,
        @Serializable(with = LocalDateTimeSerializer::class)
        val statusSistOppdatert: LocalDateTime?,
        val status: FakturaStatusType?,
    ) {
        fun erUtbetalt() = status in setOf(FakturaStatusType.DELVIS_BETALT, FakturaStatusType.FULLT_BETALT)
    }
}
