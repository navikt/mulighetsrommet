package no.nav.mulighetsrommet.api.utbetaling.db

import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import java.util.UUID

data class UtbetalingTilstandsendringDbo(
    val totrinnskontrollId: UUID,
    val returnert: UtbetalingStatusType,
    val godkjent: UtbetalingStatusType,
)
