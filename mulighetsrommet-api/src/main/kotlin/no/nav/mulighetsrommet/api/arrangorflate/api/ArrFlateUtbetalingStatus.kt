package no.nav.mulighetsrommet.api.arrangorflate.api

import no.nav.mulighetsrommet.api.utbetaling.model.Delutbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling

enum class ArrFlateUtbetalingStatus {
    KLAR_FOR_GODKJENNING,
    BEHANDLES_AV_NAV,
    UTBETALT,
    VENTER_PA_ENDRING,
    OVERFORT_TIL_UTBETALING,
    ;

    companion object {
        fun fromUtbetaling(
            utbetaling: Utbetaling,
            delutbetalinger: List<Delutbetaling>,
            relevanteForslag: List<RelevanteForslag>,
        ): ArrFlateUtbetalingStatus {
            return if (delutbetalinger.isNotEmpty() && delutbetalinger.all { it.status == DelutbetalingStatus.OVERFORT_TIL_UTBETALING }) {
                OVERFORT_TIL_UTBETALING
            } else if (delutbetalinger.isNotEmpty() && delutbetalinger.all { it.status == DelutbetalingStatus.UTBETALT }) {
                UTBETALT
            } else if (utbetaling.innsender != null) {
                BEHANDLES_AV_NAV
            } else if (relevanteForslag.any { it.antallRelevanteForslag > 0 }) {
                VENTER_PA_ENDRING
            } else {
                KLAR_FOR_GODKJENNING
            }
        }
    }
}
