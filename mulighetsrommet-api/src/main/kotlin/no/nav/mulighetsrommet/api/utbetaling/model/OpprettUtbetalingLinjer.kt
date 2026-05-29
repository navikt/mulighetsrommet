package no.nav.mulighetsrommet.api.utbetaling.model

import no.nav.mulighetsrommet.model.ValutaBelop
import java.util.UUID

data class OpprettUtbetalingLinjer(
    val utbetalingId: UUID,
    val linjer: List<OpprettUtbetalingLinje>,
    val begrunnelseMindreBetalt: String?,
)

data class OpprettUtbetalingLinje(
    val id: UUID,
    val tilsagnId: UUID,
    val pris: ValutaBelop,
    val gjorOppTilsagn: Boolean,
)
