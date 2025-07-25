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
    AVBRUTT,
    ;

    companion object {
        fun fromUtbetaling(
            status: Utbetaling.UtbetalingStatus,
            delutbetalinger: List<Delutbetaling>,
            relevanteForslag: List<RelevanteForslag>,
        ): ArrFlateUtbetalingStatus = when (status) {
            Utbetaling.UtbetalingStatus.OPPRETTET -> {
                if (relevanteForslag.any { it.antallRelevanteForslag > 0 }) {
                    VENTER_PA_ENDRING
                } else {
                    KLAR_FOR_GODKJENNING
                }
            }
            Utbetaling.UtbetalingStatus.INNSENDT,
            Utbetaling.UtbetalingStatus.TIL_ATTESTERING,
            Utbetaling.UtbetalingStatus.RETURNERT,
            -> BEHANDLES_AV_NAV
            Utbetaling.UtbetalingStatus.FERDIG_BEHANDLET -> {
                if (delutbetalinger.all { it.status == DelutbetalingStatus.UTBETALT }) {
                    UTBETALT
                } else {
                    OVERFORT_TIL_UTBETALING
                }
            }
            Utbetaling.UtbetalingStatus.AVBRUTT -> AVBRUTT
        }

        fun toReadableName(status: ArrFlateUtbetalingStatus): String {
            return when (status) {
                KLAR_FOR_GODKJENNING -> "Klar for godkjenning"
                BEHANDLES_AV_NAV -> "Behandles av NAV"
                UTBETALT -> "Utbetalt"
                VENTER_PA_ENDRING -> "Venter på endring"
                OVERFORT_TIL_UTBETALING -> "Overført til utbetaling"
                AVBRUTT -> "Avbrutt"
            }
        }
    }
}
