package no.nav.mulighetsrommet.api.utbetaling.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnDto
import no.nav.mulighetsrommet.api.totrinnskontroll.api.TotrinnskontrollDto
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.*

@Serializable
data class UtbetalingDetaljerDto(
    val utbetaling: UtbetalingDto,
    val linjer: List<UtbetalingLinje>,
    val handlinger: Set<UtbetalingHandling>,
)

@Serializable
enum class UtbetalingHandling {
    SEND_TIL_ATTESTERING,
}

@Serializable
data class UtbetalingLinje(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val tilsagn: TilsagnDto,
    val status: DelutbetalingStatus?,
    val belop: Int,
    val gjorOppTilsagn: Boolean,
    val opprettelse: TotrinnskontrollDto?,
    val handlinger: Set<UtbetalingLinjeHandling>,
)

@Serializable
enum class UtbetalingLinjeHandling {
    ATTESTER,
    RETURNER,
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
