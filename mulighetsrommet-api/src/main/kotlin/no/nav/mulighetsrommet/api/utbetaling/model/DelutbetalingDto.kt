package no.nav.mulighetsrommet.api.utbetaling.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.*

@Serializable
data class DelutbetalingDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val tilsagnId: UUID,
    @Serializable(with = UUIDSerializer::class)
    val utbetalingId: UUID,
    val status: DelutbetalingStatus,
    val periode: Periode,
    val belop: Int,
    val frigjorTilsagn: Boolean,
    val lopenummer: Int,
    val fakturanummer: String,
)

enum class DelutbetalingStatus {
    TIL_GODKJENNING,
    GODKJENT,
    RETURNERT,
    UTBETALT,
}
