package no.nav.mulighetsrommet.api.utbetaling.api

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
        fun fromUtbetalingStatus(
            status: Utbetaling.UtbetalingStatus,
        ): AdminUtbetalingStatus {
            return when (status) {
                Utbetaling.UtbetalingStatus.OPPRETTET -> VENTER_PA_ARRANGOR
                Utbetaling.UtbetalingStatus.INNSENDT -> KLAR_TIL_BEHANDLING
                Utbetaling.UtbetalingStatus.TIL_ATTESTERING -> TIL_ATTESTERING
                Utbetaling.UtbetalingStatus.RETURNERT -> RETURNERT
                Utbetaling.UtbetalingStatus.FERDIG_BEHANDLET -> OVERFORT_TIL_UTBETALING
            }
        }
    }
}
