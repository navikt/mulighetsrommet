package no.nav.mulighetsrommet.api.utbetaling.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll

@Serializable
data class AdminUtbetalingDetaljer(
    val utbetaling: AdminUtbetalingKompakt,
    val delutbetalinger: List<OpprettetDelutbetaling>,
) {
    @Serializable
    data class OpprettetDelutbetaling(
        val delutbetaling: DelutbetalingDto,
        val opprettelse: Totrinnskontroll,
    )
}
