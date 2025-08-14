package no.nav.mulighetsrommet.api.utbetaling.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnDto
import no.nav.mulighetsrommet.api.totrinnskontroll.api.TotrinnskontrollDto
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDateTime
import java.util.*

@Serializable
data class UtbetalingDetaljerDto(
    val utbetaling: UtbetalingDto,
    val linjer: List<UtbetalingLinje>,
    val handlinger: UtbetalingHandlinger,
)

@Serializable
data class UtbetalingHandlinger(
    val avbryt: Boolean,
    val sendTilAttestering: Boolean,
    val godkjennAvbryt: Boolean,
    val sendAvbrytIRetur: Boolean,
)

@Serializable
data class UtbetalingLinje(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val tilsagn: TilsagnDto,
    val status: DelutbetalingStatus,
    val belop: Int,
    val gjorOppTilsagn: Boolean,
    val opprettelse: TotrinnskontrollDto,
)

@Serializable
data class ArrangorUtbetalingLinje(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val tilsagn: Tilsagn,
    val status: DelutbetalingStatus,
    @Serializable(with = LocalDateTimeSerializer::class)
    val statusSistOppdatert: LocalDateTime?,
    val belop: Int,
) {
    @Serializable
    data class Tilsagn(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val bestillingsnummer: String,
    )
}

fun toReadableName(delutbetalingStatus: DelutbetalingStatus): String {
    return when (delutbetalingStatus) {
        DelutbetalingStatus.TIL_ATTESTERING -> "Til godkjenning"
        DelutbetalingStatus.GODKJENT -> "Godkjent"
        DelutbetalingStatus.RETURNERT -> "Returnert"
        DelutbetalingStatus.UTBETALT -> "Utbetalt"
        DelutbetalingStatus.OVERFORT_TIL_UTBETALING -> "Overført til utbetaling"
    }
}
