package no.nav.mulighetsrommet.api.utbetaling.api

import no.nav.mulighetsrommet.api.utbetaling.model.Delutbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling

enum class AdminUtbetalingStatus {
    UTBETALT,
    VENTER_PA_ARRANGOR,
    RETURNERT,
    TIL_GODKJENNING,
    GODKJENT,
    BEHANDLES_AV_NAV,
    ;

    companion object {
        fun fromUtbetaling(
            utbetaling: Utbetaling,
            delutbetalinger: List<Delutbetaling>,
        ): AdminUtbetalingStatus {
            return when (delutbetalinger.getOrNull(0)?.status) {
                DelutbetalingStatus.TIL_GODKJENNING -> TIL_GODKJENNING
                DelutbetalingStatus.GODKJENT -> GODKJENT
                DelutbetalingStatus.RETURNERT -> RETURNERT
                DelutbetalingStatus.UTBETALT -> UTBETALT
                null -> if (utbetaling.innsender != null) {
                    BEHANDLES_AV_NAV
                } else {
                    VENTER_PA_ARRANGOR
                }
            }
        }
    }
}
