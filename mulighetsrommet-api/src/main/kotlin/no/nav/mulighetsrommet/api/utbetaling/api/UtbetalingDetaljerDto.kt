package no.nav.mulighetsrommet.api.utbetaling.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.totrinnskontroll.api.TotrinnskontrollDto
import no.nav.mulighetsrommet.api.utbetaling.model.Delutbetaling

@Serializable
data class UtbetalingDetaljerDto(
    val utbetaling: UtbetalingDto,
    val delutbetalinger: List<DelutbetalingDto>,
)

@Serializable
data class DelutbetalingDto(
    val delutbetaling: Delutbetaling,
    val opprettelse: TotrinnskontrollDto,
)
