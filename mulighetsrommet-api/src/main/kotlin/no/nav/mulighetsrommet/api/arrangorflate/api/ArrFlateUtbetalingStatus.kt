package no.nav.mulighetsrommet.api.arrangorflate.api

import no.nav.mulighetsrommet.api.utbetaling.model.Delutbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling

enum class ArrFlateUtbetalingStatus {
    KLAR_FOR_GODKJENNING,
    BEHANDLES_AV_NAV,
    UTBETALT,
    KREVER_ENDRING,
    OVERFORT_TIL_UTBETALING,
    AVBRUTT,
    ;

    companion object {
        fun fromUtbetaling(
            status: Utbetaling.UtbetalingStatus,
            delutbetalinger: List<Delutbetaling>,
            harAdvarsler: Boolean,
        ): ArrFlateUtbetalingStatus = when (status) {
            Utbetaling.UtbetalingStatus.OPPRETTET -> {
                if (harAdvarsler) {
                    KREVER_ENDRING
                } else {
                    KLAR_FOR_GODKJENNING
                }
            }
            Utbetaling.UtbetalingStatus.TIL_AVBRYTELSE,
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
                KREVER_ENDRING -> "Krever endring"
                OVERFORT_TIL_UTBETALING -> "Overført til utbetaling"
                AVBRUTT -> "Avbrutt"
            }
        }
    }
}
