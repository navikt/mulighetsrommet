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
