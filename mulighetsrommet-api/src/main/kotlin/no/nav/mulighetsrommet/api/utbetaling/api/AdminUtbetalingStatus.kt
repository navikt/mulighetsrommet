package no.nav.mulighetsrommet.api.utbetaling.api

import no.nav.mulighetsrommet.api.utbetaling.model.Delutbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling

enum class AdminUtbetalingStatus {
    UTBETALT,
    VENTER_PA_ARRANGOR,
    RETURNERT,
    TIL_ATTESTERING,
    KLAR_TIL_BEHANDLING,
    OVERFORT_TIL_UTBETALING,
    ;

    companion object {
        fun fromUtbetaling(
            utbetaling: Utbetaling,
            delutbetalinger: List<Delutbetaling>,
        ): AdminUtbetalingStatus {
            if (delutbetalinger.isNotEmpty() && delutbetalinger.all { it.status === DelutbetalingStatus.GODKJENT }) {
                return OVERFORT_TIL_UTBETALING
            }

            return when (delutbetalinger.getOrNull(0)?.status) {
                DelutbetalingStatus.TIL_ATTESTERING, DelutbetalingStatus.GODKJENT -> TIL_ATTESTERING
                DelutbetalingStatus.RETURNERT -> RETURNERT
                DelutbetalingStatus.UTBETALT -> UTBETALT
                DelutbetalingStatus.OVERFORT_TIL_UTBETALING -> OVERFORT_TIL_UTBETALING
                null -> if (utbetaling.innsender != null) {
                    KLAR_TIL_BEHANDLING
                } else {
                    VENTER_PA_ARRANGOR
                }
            }
        }
    }
}
