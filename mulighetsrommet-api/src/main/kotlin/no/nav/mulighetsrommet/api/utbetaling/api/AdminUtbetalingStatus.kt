package no.nav.mulighetsrommet.api.utbetaling.api

import no.nav.mulighetsrommet.api.utbetaling.model.Delutbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling

enum class AdminUtbetalingStatus {
    UTBETALT,
    VENTER_PA_ARRANGOR,
    BEHANDLES_AV_NAV,
    ;

    companion object {
        fun fromUtbetaling(
            utbetaling: Utbetaling,
            delutbetalinger: List<Delutbetaling>,
        ): AdminUtbetalingStatus {
            return if (delutbetalinger.isNotEmpty() && delutbetalinger.all { it.status == DelutbetalingStatus.UTBETALT }) {
                UTBETALT
            } else if (utbetaling.innsender != null) {
                BEHANDLES_AV_NAV
            } else {
                VENTER_PA_ARRANGOR
            }
        }
    }
}
